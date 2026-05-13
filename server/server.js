/**
 * ConnectChat - WebSocket Relay Server v2
 * Phase 1: typing indicator, online/offline presence, read receipts,
 *           delete-for-everyone, and user-list discovery.
 */

const { WebSocketServer } = require("ws");

const PORT = process.env.PORT || 8080;
const wss = new WebSocketServer({ port: PORT });

// Map: username -> { ws, lastSeen }
const users = new Map();

function broadcast(payload, excludeUsername = null) {
  const text = JSON.stringify(payload);
  for (const [username, { ws }] of users) {
    if (username !== excludeUsername && ws.readyState === 1 /* OPEN */) {
      ws.send(text);
    }
  }
}

function sendTo(username, payload) {
  const entry = users.get(username);
  if (entry && entry.ws.readyState === 1) {
    entry.ws.send(JSON.stringify(payload));
    return true;
  }
  return false;
}

console.log(`ConnectChat relay server v2 running on port ${PORT}`);

wss.on("connection", (ws) => {
  let registeredUsername = null;

  ws.on("message", (data) => {
    try {
      const msg = JSON.parse(data.toString());

      // ---------- REGISTER ----------
      if (msg.type === "register") {
        registeredUsername = msg.username;
        users.set(registeredUsername, { ws, lastSeen: Date.now() });
        console.log(`[+] ${registeredUsername} connected (${users.size} online)`);

        // Tell everyone this user is now online
        broadcast({ type: "user_online", username: registeredUsername }, registeredUsername);

        // Tell the newly connected user who else is already online
        ws.send(JSON.stringify({
          type: "online_list",
          users: [...users.keys()].filter(u => u !== registeredUsername),
        }));
        return;
      }

      // All other events require registration
      if (!registeredUsername) return;

      // ---------- SEND MESSAGE ----------
      if (msg.type === "message") {
        const delivered = sendTo(msg.to, {
          type: "message",
          from: registeredUsername,
          content: msg.content,
          messageType: msg.messageType || "text",
          timestamp: msg.timestamp || Date.now(),
        });
        if (!delivered) {
          console.log(`[-] ${msg.to} is offline, message dropped`);
        }
        return;
      }

      // ---------- TYPING INDICATOR ----------
      if (msg.type === "typing") {
        sendTo(msg.to, { type: "typing", from: registeredUsername });
        return;
      }

      // ---------- READ RECEIPT ----------
      // Tells the original sender their messages have been read
      if (msg.type === "read") {
        sendTo(msg.to, {
          type: "read",
          from: registeredUsername,
          conversationId: msg.conversationId,
          upToTimestamp: msg.upToTimestamp,
        });
        return;
      }

      // ---------- DELETE MESSAGE ----------
      // Tells the peer to remove a specific message
      if (msg.type === "delete") {
        sendTo(msg.to, {
          type: "delete",
          from: registeredUsername,
          conversationId: msg.conversationId,
          timestamp: msg.timestamp,
        });
        return;
      }

      // ---------- GET USER LIST (contact search) ----------
      if (msg.type === "get_users") {
        ws.send(JSON.stringify({
          type: "user_list",
          users: [...users.keys()].filter(u => u !== registeredUsername),
        }));
        return;
      }

    } catch (err) {
      console.error("Invalid message:", err.message);
    }
  });

  ws.on("close", () => {
    if (registeredUsername) {
      const lastSeen = Date.now();
      users.delete(registeredUsername);
      console.log(`[-] ${registeredUsername} disconnected (${users.size} online)`);

      // Tell everyone this user went offline
      broadcast({
        type: "user_offline",
        username: registeredUsername,
        lastSeen,
      });
    }
  });

  ws.on("error", (err) => {
    console.error("WebSocket error:", err.message);
  });
});
