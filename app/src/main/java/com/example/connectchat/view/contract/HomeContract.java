package com.example.connectchat.view.contract;

import com.example.connectchat.model.db.entity.Conversation;

import java.util.List;

public interface HomeContract {

    interface View {
        void showConversations(List<Conversation> conversations);
        void showEmpty();
        void onNewConversationStarted(String peerUsername);
    }

    interface Presenter {
        void loadConversations(String username);
        void startConversation(String currentUser, String peerUsername);
        void destroy();
    }
}
