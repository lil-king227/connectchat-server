package com.example.connectchat.presenter;

import android.content.Context;

import com.example.connectchat.model.db.AppDatabase;
import com.example.connectchat.model.db.entity.User;
import com.example.connectchat.view.contract.RegisterContract;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterPresenter implements RegisterContract.Presenter {

    private RegisterContract.View view;
    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public RegisterPresenter(RegisterContract.View view, Context context) {
        this.view = view;
        this.db = AppDatabase.getInstance(context);
    }

    @Override
    public void register(String username, String password, String confirmPassword) {
        username = username.trim();

        if (username.isEmpty()) {
            view.onRegisterFailed("Username cannot be empty");
            return;
        }
        if (username.length() < 3) {
            view.onRegisterFailed("Username must be at least 3 characters");
            return;
        }
        if (password.length() < 4) {
            view.onRegisterFailed("Password must be at least 4 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            view.onRegisterFailed("Passwords do not match");
            return;
        }

        view.showLoading();
        String finalUsername = username;
        executor.execute(() -> {
            User existing = db.userDao().findByUsername(finalUsername);
            if (view == null) return;
            if (existing != null) {
                view.onRegisterFailed("Username already taken");
            } else {
                User newUser = new User(finalUsername, password, System.currentTimeMillis());
                db.userDao().insert(newUser);
                view.onRegisterSuccess();
            }
        });
    }

    @Override
    public void destroy() {
        view = null;
        executor.shutdown();
    }
}
