package com.example.connectchat.view.contract;

import com.example.connectchat.model.db.entity.Message;

import java.util.List;

public interface ChatContract {

    interface View {
        void showMessages(List<Message> messages);
        void appendMessage(Message message);
        void showConnectionStatus(boolean connected);
        void onImageSent(Message message);
        void onConversationDeleted();

        // Phase 1 additions
        /** Show or hide the "peer is typing…" indicator. */
        void showTypingIndicator(boolean visible);
        /** A message the peer sent was deleted remotely — remove it from the list. */
        void onRemoteMessageDeleted(String senderId, long timestamp);
        /** Update the status (✓/✓✓) of a sent message after a read receipt. */
        void onReadReceiptUpdated(String conversationId, long upToTimestamp);
    }

    interface Presenter {
        void loadHistory(String conversationId);
        void sendTextMessage(String conversationId, String senderId,
                             String receiverId, String content);
        void sendImageMessage(String conversationId, String senderId,
                              String receiverId, String imagePath);
        void clearHistory(String conversationId);
        void deleteConversation(String conversationId);
        void destroy();

        // Phase 1 additions
        void sendTypingEvent();
        /** Delete one of our own messages for both sides. */
        void deleteMessage(Message message);
        /** Send read receipt for all received messages up to the latest timestamp. */
        void sendReadReceipts(String conversationId, String peerUser, long latestTimestamp);
    }
}
