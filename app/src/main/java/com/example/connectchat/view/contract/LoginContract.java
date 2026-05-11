package com.example.connectchat.view.contract;

public interface LoginContract {

    interface View {
        void showLoading();
        void hideLoading();
        void onLoginSuccess(String username);
        void onLoginFailed(String message);
    }

    interface Presenter {
        void login(String username, String password);
        void destroy();
    }
}
