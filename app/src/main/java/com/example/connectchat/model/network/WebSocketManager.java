package com.example.connectchat.model.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.util.concurrent.TimeUnit;

/**
 * Singleton WebSocket manager.
 * Connects to the relay server and routes messages to/from the app.
 */
public class WebSocketManager {

    private static final String TAG = "WebSocketManager";

    // ---------------------------------------------------------------
    // Replace this URL once you deploy your relay server on Render.com
    // ---------------------------------------------------------------
    public static final String SERVER_URL = "wss://connectchat-relay.onrender.com";

    public interface MessageListener {
        void onTextMessageReceived(String fromUser, String content, long timestamp);
        void onImageMessageReceived(String fromUser, String imagePath, long timestamp);
        void onConnected();
        void onDisconnected();
    }

    private static WebSocketManager instance;
    private WebSocket webSocket;
    private OkHttpClient client;
    private MessageListener listener;
    private String currentUsername;
    private boolean connected = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    private WebSocketManager() {
        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)  // no timeout for WS
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

    public void connect(String username, MessageListener listener) {
        this.currentUsername = username;
        this.listener = listener;

        Request request = new Request.Builder().url(SERVER_URL).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket ws, Response response) {
                connected = true;
                Log.d(TAG, "Connected to relay server");
                // Register our username with the server
                JsonObject reg = new JsonObject();
                reg.addProperty("type", "register");
                reg.addProperty("username", username);
                ws.send(reg.toString());

                mainHandler.post(() -> {
                    if (listener != null) listener.onConnected();
                });
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                Log.d(TAG, "Message received: " + text);
                try {
                    JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                    String type    = json.get("type").getAsString();
                    String from    = json.has("from") ? json.get("from").getAsString() : "";
                    String content = json.has("content") ? json.get("content").getAsString() : "";
                    long ts        = json.has("timestamp") ? json.get("timestamp").getAsLong()
                                                           : System.currentTimeMillis();

                    mainHandler.post(() -> {
                        if (listener == null) return;
                        if ("message".equals(type)) {
                            String msgType = json.has("messageType")
                                    ? json.get("messageType").getAsString() : "text";
                            if ("image".equals(msgType)) {
                                listener.onImageMessageReceived(from, content, ts);
                            } else {
                                listener.onTextMessageReceived(from, content, ts);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse message", e);
                }
            }

            @Override
            public void onMessage(WebSocket ws, ByteString bytes) {
                // not used
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                connected = false;
                ws.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                connected = false;
                mainHandler.post(() -> {
                    if (listener != null) listener.onDisconnected();
                });
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                connected = false;
                Log.e(TAG, "WebSocket failure", t);
                mainHandler.post(() -> {
                    if (listener != null) listener.onDisconnected();
                });
                // Auto-reconnect after 3 seconds
                mainHandler.postDelayed(() -> {
                    if (currentUsername != null) {
                        connect(currentUsername, WebSocketManager.this.listener);
                    }
                }, 3000);
            }
        });
    }

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

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "User logged out");
            webSocket = null;
        }
        connected = false;
        listener = null;
        currentUsername = null;
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }
}
