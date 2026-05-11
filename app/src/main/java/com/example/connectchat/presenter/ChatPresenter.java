package com.example.connectchat.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.connectchat.model.db.AppDatabase;
import com.example.connectchat.model.db.entity.Conversation;
import com.example.connectchat.model.db.entity.Message;
import com.example.connectchat.model.network.WebSocketManager;
import com.example.connectchat.view.contract.ChatContract;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatPresenter implements ChatContract.Presenter, WebSocketManager.MessageListener {

    private ChatContract.View view;
    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String currentUser;
    private final String peerUser;

    public ChatPresenter(ChatContract.View view, Context context,
                         String currentUser, String peerUser) {
        this.view = view;
        this.db = AppDatabase.getInstance(context);
        this.currentUser = currentUser;
        this.peerUser = peerUser;

        // Hook into the already-connected WebSocket
        WebSocketManager.getInstance().setListener(this);
    }

    @Override
    public void loadHistory(String conversationId) {
        executor.execute(() -> {
            List<Message> messages = db.messageDao().getMessagesForConversation(conversationId);
            // Mark as read
            db.conversationDao().markAsRead(conversationId);
            if (view != null) {
                view.showMessages(messages);
            }
        });
    }

    @Override
    public void sendTextMessage(String conversationId, String senderId,
                                String receiverId, String content) {
        if (content.trim().isEmpty()) return;

        Message msg = new Message(conversationId, senderId, receiverId,
                content.trim(), Message.TYPE_TEXT, Message.STATUS_SENT,
                System.currentTimeMillis());

        executor.execute(() -> {
            db.messageDao().insert(msg);
            updateConversationLastMessage(conversationId, content.trim());
        });

        // Send via WebSocket
        WebSocketManager.getInstance().sendText(receiverId, content.trim());

        if (view != null) {
            view.appendMessage(msg);
        }
    }

    @Override
    public void sendImageMessage(String conversationId, String senderId,
                                 String receiverId, String imagePath) {
        Message msg = new Message(conversationId, senderId, receiverId,
                imagePath, Message.TYPE_IMAGE, Message.STATUS_SENT,
                System.currentTimeMillis());

        executor.execute(() -> {
            db.messageDao().insert(msg);
            updateConversationLastMessage(conversationId, "[Image]");
        });

        // Send via WebSocket
        WebSocketManager.getInstance().sendImage(receiverId, imagePath);

        if (view != null) {
            view.onImageSent(msg);
        }
    }

    @Override
    public void clearHistory(String conversationId) {
        executor.execute(() -> {
            db.messageDao().deleteByConversation(conversationId);
            if (view != null) {
                view.showMessages(java.util.Collections.emptyList());
            }
        });
    }

    private void updateConversationLastMessage(String conversationId, String lastMsg) {
        Conversation conv = db.conversationDao().findById(conversationId);
        if (conv != null) {
            conv.lastMessage = lastMsg;
            conv.lastMessageTime = System.currentTimeMillis();
            db.conversationDao().update(conv);
        }
    }

    // --- WebSocketManager.MessageListener ---

    @Override
    public void onTextMessageReceived(String fromUser, String content, long timestamp) {
        if (!fromUser.equals(peerUser)) return; // message is not from our peer
        String conversationId = Conversation.buildId(currentUser, peerUser);
        Message msg = new Message(conversationId, fromUser, currentUser,
                content, Message.TYPE_TEXT, Message.STATUS_RECEIVED, timestamp);

        executor.execute(() -> {
            db.messageDao().insert(msg);
            updateConversationLastMessage(conversationId, content);
        });

        mainHandler.post(() -> {
            if (view != null) view.appendMessage(msg);
        });
    }

    @Override
    public void onImageMessageReceived(String fromUser, String imagePath, long timestamp) {
        if (!fromUser.equals(peerUser)) return;
        String conversationId = Conversation.buildId(currentUser, peerUser);
        Message msg = new Message(conversationId, fromUser, currentUser,
                imagePath, Message.TYPE_IMAGE, Message.STATUS_RECEIVED, timestamp);

        executor.execute(() -> {
            db.messageDao().insert(msg);
            updateConversationLastMessage(conversationId, "[Image]");
        });

        mainHandler.post(() -> {
            if (view != null) view.appendMessage(msg);
        });
    }

    @Override
    public void onConnected() {
        mainHandler.post(() -> {
            if (view != null) view.showConnectionStatus(true);
        });
    }

    @Override
    public void onDisconnected() {
        mainHandler.post(() -> {
            if (view != null) view.showConnectionStatus(false);
        });
    }

    @Override
    public void destroy() {
        view = null;
        executor.shutdown();
        // Restore a no-op listener so home screen still gets messages
        WebSocketManager.getInstance().setListener(null);
    }
}
