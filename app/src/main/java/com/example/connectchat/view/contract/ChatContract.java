package com.example.connectchat.view.contract;

import com.example.connectchat.model.db.entity.Message;

import java.util.List;

public interface ChatContract {

    interface View {
        void showMessages(List<Message> messages);
        void appendMessage(Message message);
        void showConnectionStatus(boolean connected);
        void onImageSent(Message message);
    }

    interface Presenter {
        void loadHistory(String conversationId);
        void sendTextMessage(String conversationId, String senderId,
                             String receiverId, String content);
        void sendImageMessage(String conversationId, String senderId,
                              String receiverId, String imagePath);
        void clearHistory(String conversationId);
        void destroy();
    }
}
