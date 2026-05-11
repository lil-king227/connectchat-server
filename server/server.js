/**
 * ConnectChat - WebSocket Relay Server
 * Simple server that routes messages between registered users.
 * Deploy for free on Render.com (see README in this folder).
 */

const { WebSocketServer } = require("ws");

const PORT = process.env.PORT || 8080;
const wss = new WebSocketServer({ port: PORT });

// Map: username -> WebSocket connection
const users = new Map();

console.log(`ConnectChat relay server running on port ${PORT}`);

wss.on("connection", (ws) => {
  let registeredUsername = null;

  ws.on("message", (data) => {
    try {
      const msg = JSON.parse(data.toString());

      // ---------- REGISTER ----------
      if (msg.type === "register") {
        registeredUsername = msg.username;
        users.set(registeredUsername, ws);
        console.log(`[+] ${registeredUsername} connected (${users.size} online)`);
        return;
      }

      // ---------- SEND MESSAGE ----------
      if (msg.type === "message") {
        const targetWs = users.get(msg.to);
        if (targetWs && targetWs.readyState === 1 /* OPEN */) {
          targetWs.send(
            JSON.stringify({
              type: "message",
              from: registeredUsername,
              content: msg.content,
              messageType: msg.messageType || "text",
              timestamp: msg.timestamp || Date.now(),
            })
          );
        } else {
          console.log(`[-] ${msg.to} is offline, message dropped`);
        }
      }
    } catch (err) {
      console.error("Invalid message:", err.message);
    }
  });

  ws.on("close", () => {
    if (registeredUsername) {
      users.delete(registeredUsername);
      console.log(`[-] ${registeredUsername} disconnected (${users.size} online)`);
    }
  });

  ws.on("error", (err) => {
    console.error("WebSocket error:", err.message);
  });
});
