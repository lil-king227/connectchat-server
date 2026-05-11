package com.example.connectchat.presenter;

import android.content.Context;

import com.example.connectchat.model.db.AppDatabase;
import com.example.connectchat.model.db.entity.Conversation;
import com.example.connectchat.view.contract.HomeContract;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomePresenter implements HomeContract.Presenter {

    private HomeContract.View view;
    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HomePresenter(HomeContract.View view, Context context) {
        this.view = view;
        this.db = AppDatabase.getInstance(context);
    }

    @Override
    public void loadConversations(String username) {
        executor.execute(() -> {
            List<Conversation> convs = db.conversationDao().getConversationsForUser(username);
            if (view == null) return;
            if (convs.isEmpty()) {
                view.showEmpty();
            } else {
                view.showConversations(convs);
            }
        });
    }

    @Override
    public void startConversation(String currentUser, String peerUsername) {
        if (peerUsername.trim().isEmpty()) return;
        if (peerUsername.trim().equalsIgnoreCase(currentUser)) {
            return; // can't chat with yourself
        }

        executor.execute(() -> {
            String convId = Conversation.buildId(currentUser, peerUsername.trim());
            Conversation existing = db.conversationDao().findById(convId);
            if (existing == null) {
                Conversation newConv = new Conversation(
                        convId, currentUser, peerUsername.trim(),
                        "", System.currentTimeMillis(), 0
                );
                db.conversationDao().insert(newConv);
            }
            if (view != null) {
                view.onNewConversationStarted(peerUsername.trim());
            }
        });
    }

    @Override
    public void destroy() {
        view = null;
        executor.shutdown();
    }
}
