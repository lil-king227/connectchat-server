package com.example.connectchat.model.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WebSocketManager {

    private static final String TAG = "WebSocketManager";

    public static final String SERVER_URL = "wss://connectchat-server-4j5i.onrender.com/";

    // -----------------------------------------------------------------------
    // Listener interface — all callbacks arrive on the MAIN thread.
    // Default (no-op) implementations keep existing code unmodified.
    // -----------------------------------------------------------------------
    public interface MessageListener {
        // --- existing ---
        void onTextMessageReceived(String fromUser, String content, long timestamp);
        void onImageMessageReceived(String fromUser, String imagePath, long timestamp);
        void onConnected();
        void onDisconnected();

        // --- Phase 1: typing ---
        default void onTypingReceived(String fromUser) {}

        // --- Phase 1: presence ---
        default void onUserOnline(String username) {}
        default void onUserOffline(String username, long lastSeen) {}
        default void onOnlineListReceived(List<String> onlineUsers) {}

        // --- Phase 1: read receipts ---
        default void onReadReceiptReceived(String fromUser, String conversationId, long upToTimestamp) {}

        // --- Phase 1: delete ---
        default void onMessageDeleteReceived(String fromUser, String conversationId, long timestamp) {}

        // --- Phase 1: contact search ---
        default void onUserListReceived(List<String> users) {}
    }

    // -----------------------------------------------------------------------

    private static WebSocketManager instance;
    private WebSocket webSocket;
    private final OkHttpClient client;

    private volatile MessageListener listener;
    private String currentUsername;
    private volatile boolean connected = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private WebSocketManager() {
        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public static WebSocketManager getInstance() {
        if (instance == null) {
            synchronized (WebSocketManager.class) {
                if (instance == null) instance = new WebSocketManager();
            }
        }
        return instance;
    }

    // -----------------------------------------------------------------------
    // Connect / disconnect
    // -----------------------------------------------------------------------

    public void connect(String username, MessageListener initialListener) {
        this.currentUsername = username;
        this.listener = initialListener;

        if (webSocket != null) {
            webSocket.cancel();
            webSocket = null;
        }

        Request request = new Request.Builder().url(SERVER_URL).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket ws, Response response) {
                connected = true;
                Log.d(TAG, "Connected as " + username);
                JsonObject reg = new JsonObject();
                reg.addProperty("type", "register");
                reg.addProperty("username", username);
                ws.send(reg.toString());

                mainHandler.post(() -> {
                    if (WebSocketManager.this.listener != null)
                        WebSocketManager.this.listener.onConnected();
                });
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                Log.d(TAG, "Received: " + text);
                try {
                    JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                    String type = json.get("type").getAsString();

                    mainHandler.post(() -> {
                        MessageListener cur = WebSocketManager.this.listener;
                        if (cur == null) return;

                        switch (type) {

                            case "message": {
                                String from    = json.has("from")    ? json.get("from").getAsString()    : "";
                                String content = json.has("content") ? json.get("content").getAsString() : "";
                                long ts        = json.has("timestamp") ? json.get("timestamp").getAsLong() : System.currentTimeMillis();
                                String msgType = json.has("messageType") ? json.get("messageType").getAsString() : "text";
                                if ("image".equals(msgType)) {
                                    cur.onImageMessageReceived(from, content, ts);
                                } else {
                                    cur.onTextMessageReceived(from, content, ts);
                                }
                                break;
                            }

                            case "typing": {
                                String from = json.has("from") ? json.get("from").getAsString() : "";
                                cur.onTypingReceived(from);
                                break;
                            }

                            case "user_online": {
                                String username2 = json.has("username") ? json.get("username").getAsString() : "";
                                cur.onUserOnline(username2);
                                break;
                            }

                            case "user_offline": {
                                String username2 = json.has("username") ? json.get("username").getAsString() : "";
                                long lastSeen = json.has("lastSeen") ? json.get("lastSeen").getAsLong() : 0;
                                cur.onUserOffline(username2, lastSeen);
                                break;
                            }

                            case "online_list": {
                                List<String> onlineUsers = new ArrayList<>();
                                if (json.has("users")) {
                                    for (com.google.gson.JsonElement el : json.get("users").getAsJsonArray()) {
                                        onlineUsers.add(el.getAsString());
                                    }
                                }
                                cur.onOnlineListReceived(onlineUsers);
                                break;
                            }

                            case "read": {
                                String from   = json.has("from")   ? json.get("from").getAsString()   : "";
                                String convId = json.has("conversationId") ? json.get("conversationId").getAsString() : "";
                                long upTo     = json.has("upToTimestamp") ? json.get("upToTimestamp").getAsLong() : 0;
                                cur.onReadReceiptReceived(from, convId, upTo);
                                break;
                            }

                            case "delete": {
                                String from      = json.has("from")      ? json.get("from").getAsString()      : "";
                                String convId    = json.has("conversationId") ? json.get("conversationId").getAsString() : "";
                                long   timestamp = json.has("timestamp") ? json.get("timestamp").getAsLong()   : 0;
                                cur.onMessageDeleteReceived(from, convId, timestamp);
                                break;
                            }

                            case "user_list": {
                                List<String> users = new ArrayList<>();
                                if (json.has("users")) {
                                    for (com.google.gson.JsonElement el : json.get("users").getAsJsonArray()) {
                                        users.add(el.getAsString());
                                    }
                                }
                                cur.onUserListReceived(users);
                                break;
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse message", e);
                }
            }

            @Override
            public void onMessage(WebSocket ws, ByteString bytes) { }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                connected = false;
                ws.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                connected = false;
                mainHandler.post(() -> {
                    if (WebSocketManager.this.listener != null)
                        WebSocketManager.this.listener.onDisconnected();
                });
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                connected = false;
                Log.e(TAG, "Connection failed: " + t.getMessage());
                mainHandler.post(() -> {
                    if (WebSocketManager.this.listener != null)
                        WebSocketManager.this.listener.onDisconnected();
                });
                // Auto-reconnect after 4 seconds
                mainHandler.postDelayed(() -> {
                    if (currentUsername != null && webSocket == ws) {
                        webSocket = null;
                        Log.d(TAG, "Reconnecting as " + currentUsername);
                        connect(currentUsername, WebSocketManager.this.listener);
                    }
                }, 4000);
            }
        });
    }

    // -----------------------------------------------------------------------
    // Send helpers
    // -----------------------------------------------------------------------

    public void sendText(String toUser, String content) {
        if (!connected || webSocket == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "message");
        msg.addProperty("to", toUser);
        msg.addProperty("content", content);
        msg.addProperty("messageType", "text");
        msg.addProperty("timestamp", System.currentTimeMillis());
        webSocket.send(msg.toString());
    }

    public void sendImage(String toUser, String imagePath) {
        if (!connected || webSocket == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "message");
        msg.addProperty("to", toUser);
        msg.addProperty("content", imagePath);
        msg.addProperty("messageType", "image");
        msg.addProperty("timestamp", System.currentTimeMillis());
        webSocket.send(msg.toString());
    }

    /** Send "typing" event — fire-and-forget, no DB. */
    public void sendTyping(String toUser) {
        if (!connected || webSocket == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "typing");
        msg.addProperty("to", toUser);
        webSocket.send(msg.toString());
    }

    /** Tell the sender that we have read messages up to upToTimestamp. */
    public void sendReadReceipt(String toUser, String conversationId, long upToTimestamp) {
        if (!connected || webSocket == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "read");
        msg.addProperty("to", toUser);
        msg.addProperty("conversationId", conversationId);
        msg.addProperty("upToTimestamp", upToTimestamp);
        webSocket.send(msg.toString());
    }

    /** Tell the peer to delete a specific message (identified by timestamp+sender). */
    public void sendDeleteMessage(String toUser, String conversationId, long timestamp) {
        if (!connected || webSocket == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "delete");
        msg.addProperty("to", toUser);
        msg.addProperty("conversationId", conversationId);
        msg.addProperty("timestamp", timestamp);
        webSocket.send(msg.toString());
    }

    /** Request full list of currently online users from the server. */
    public void requestUserList() {
        if (!connected || webSocket == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "get_users");
        webSocket.send(msg.toString());
    }

    // -----------------------------------------------------------------------
    // Misc
    // -----------------------------------------------------------------------

    public boolean isConnected() { return connected; }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Logout");
            webSocket = null;
        }
        connected = false;
        listener = null;
        currentUsername = null;
    }
}
