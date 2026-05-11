package com.example.connectchat.presenter;

import android.content.Context;

import com.example.connectchat.model.db.AppDatabase;
import com.example.connectchat.model.db.entity.User;
import com.example.connectchat.view.contract.LoginContract;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginPresenter implements LoginContract.Presenter {

    private LoginContract.View view;
    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LoginPresenter(LoginContract.View view, Context context) {
        this.view = view;
        this.db = AppDatabase.getInstance(context);
    }

    @Override
    public void login(String username, String password) {
        if (username.trim().isEmpty()) {
            view.onLoginFailed("Username cannot be empty");
            return;
        }
        if (password.isEmpty()) {
            view.onLoginFailed("Password cannot be empty");
            return;
        }

        view.showLoading();
        executor.execute(() -> {
            User user = db.userDao().login(username.trim(), password);
            if (view == null) return;
            // Post result back — view will handle UI thread
            if (user != null) {
                view.onLoginSuccess(user.username);
            } else {
                view.onLoginFailed("Invalid username or password");
            }
        });
    }

    @Override
    public void destroy() {
        view = null;
        executor.shutdown();
    }
}
