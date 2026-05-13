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

    // Typing indicator: auto-hide after 3 s of silence
    private Runnable hideTypingRunnable;

    public ChatPresenter(ChatContract.View view, Context context,
                         String currentUser, String peerUser) {
        this.view = view;
        this.db = AppDatabase.getInstance(context);
        this.currentUser = currentUser;
        this.peerUser = peerUser;

        WebSocketManager.getInstance().setListener(this);
    }

    // -----------------------------------------------------------------------
    // History & messages
    // -----------------------------------------------------------------------

    @Override
    public void loadHistory(String conversationId) {
        executor.execute(() -> {
            List<Message> messages = db.messageDao().getMessagesForConversation(conversationId);
            db.conversationDao().markAsRead(conversationId);
            mainHandler.post(() -> {
                if (view != null) view.showMessages(messages);
            });
        });
    }

    @Override
    public void sendTextMessage(String conversationId, String senderId,
                                String receiverId, String content) {
        if (content.trim().isEmpty()) return;

        Message msg = new Message(conversationId, senderId, receiverId,
                content.trim(), Message.TYPE_TEXT, Message.STATUS_SENT,
                System.currentTimeMillis());

        if (view != null) view.appendMessage(msg);

        executor.execute(() -> {
            db.messageDao().insert(msg);
            updateConversationLastMessage(conversationId, content.trim());
        });

        WebSocketManager.getInstance().sendText(receiverId, content.trim());
    }

    @Override
    public void sendImageMessage(String conversationId, String senderId,
                                 String receiverId, String imagePath) {
        Message msg = new Message(conversationId, senderId, receiverId,
                imagePath, Message.TYPE_IMAGE, Message.STATUS_SENT,
                System.currentTimeMillis());

        if (view != null) view.onImageSent(msg);

        executor.execute(() -> {
            db.messageDao().insert(msg);
            updateConversationLastMessage(conversationId, "[Image]");
        });

        WebSocketManager.getInstance().sendImage(receiverId, imagePath);
    }

    @Override
    public void clearHistory(String conversationId) {
        executor.execute(() -> {
            db.messageDao().deleteByConversation(conversationId);
            mainHandler.post(() -> {
                if (view != null) view.showMessages(java.util.Collections.emptyList());
            });
        });
    }

    @Override
    public void deleteConversation(String conversationId) {
        executor.execute(() -> {
            db.messageDao().deleteByConversation(conversationId);
            db.conversationDao().deleteById(conversationId);
            mainHandler.post(() -> {
                if (view != null) view.onConversationDeleted();
            });
        });
    }

    // -----------------------------------------------------------------------
    // Phase 1: typing
    // -----------------------------------------------------------------------

    @Override
    public void sendTypingEvent() {
        WebSocketManager.getInstance().sendTyping(peerUser);
    }

    // -----------------------------------------------------------------------
    // Phase 1: delete message
    // -----------------------------------------------------------------------

    @Override
    public void deleteMessage(Message message) {
        String conversationId = message.conversationId;
        long timestamp = message.timestamp;

        // 1. Update local DB
        executor.execute(() -> {
            db.messageDao().markAsDeleted(currentUser, conversationId, timestamp);
        });

        // 2. Notify peer over WebSocket
        WebSocketManager.getInstance().sendDeleteMessage(peerUser, conversationId, timestamp);

        // 3. Update UI immediately
        if (view != null) view.onRemoteMessageDeleted(currentUser, timestamp);
    }

    // -----------------------------------------------------------------------
    // Phase 1: read receipts
    // -----------------------------------------------------------------------

    @Override
    public void sendReadReceipts(String conversationId, String peerUser, long latestTimestamp) {
        if (latestTimestamp <= 0) return;
        WebSocketManager.getInstance().sendReadReceipt(peerUser, conversationId, latestTimestamp);
    }

    // -----------------------------------------------------------------------
    // WebSocketManager.MessageListener — all on MAIN thread
    // -----------------------------------------------------------------------

    @Override
    public void onTextMessageReceived(String fromUser, String content, long timestamp) {
        if (!fromUser.equalsIgnoreCase(peerUser)) return;

        String conversationId = Conversation.buildId(currentUser, peerUser);
        Message msg = new Message(conversationId, fromUser, currentUser,
                content, Message.TYPE_TEXT, Message.STATUS_RECEIVED, timestamp);

        if (view != null) view.appendMessage(msg);

        executor.execute(() -> {
            db.messageDao().insert(msg);
            updateConversationLastMessage(conversationId, content);
        });
    }

    @Override
    public void onImageMessageReceived(String fromUser, String imagePath, long timestamp) {
        if (!fromUser.equalsIgnoreCase(peerUser)) return;

        String conversationId = Conversation.buildId(currentUser, peerUser);
        Message msg = new Message(conversationId, fromUser, currentUser,
                imagePath, Message.TYPE_IMAGE, Message.STATUS_RECEIVED, timestamp);

        if (view != null) view.appendMessage(msg);

        executor.execute(() -> {
            db.messageDao().insert(msg);
            updateConversationLastMessage(conversationId, "[Image]");
        });
    }

    @Override
    public void onTypingReceived(String fromUser) {
        if (!fromUser.equalsIgnoreCase(peerUser)) return;

        if (view != null) view.showTypingIndicator(true);

        // Cancel previous hide runnable
        if (hideTypingRunnable != null) mainHandler.removeCallbacks(hideTypingRunnable);
        hideTypingRunnable = () -> {
            if (view != null) view.showTypingIndicator(false);
        };
        mainHandler.postDelayed(hideTypingRunnable, 3000);
    }

    @Override
    public void onReadReceiptReceived(String fromUser, String conversationId, long upToTimestamp) {
        if (!fromUser.equalsIgnoreCase(peerUser)) return;

        executor.execute(() -> {
            db.messageDao().markMessagesRead(conversationId, currentUser, upToTimestamp);
        });

        if (view != null) view.onReadReceiptUpdated(conversationId, upToTimestamp);
    }

    @Override
    public void onMessageDeleteReceived(String fromUser, String conversationId, long timestamp) {
        if (!fromUser.equalsIgnoreCase(peerUser)) return;

        executor.execute(() -> {
            db.messageDao().markAsDeleted(fromUser, conversationId, timestamp);
        });

        if (view != null) view.onRemoteMessageDeleted(fromUser, timestamp);
    }

    @Override
    public void onConnected() {
        if (view != null) view.showConnectionStatus(true);
    }

    @Override
    public void onDisconnected() {
        if (view != null) view.showConnectionStatus(false);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void updateConversationLastMessage(String conversationId, String lastMsg) {
        Conversation conv = db.conversationDao().findById(conversationId);
        if (conv != null) {
            conv.lastMessage = lastMsg;
            conv.lastMessageTime = System.currentTimeMillis();
            db.conversationDao().update(conv);
        }
    }

    public void reattachListener() {
        WebSocketManager.getInstance().setListener(this);
    }

    @Override
    public void destroy() {
        if (hideTypingRunnable != null) mainHandler.removeCallbacks(hideTypingRunnable);
        view = null;
        executor.shutdown();
    }
}
