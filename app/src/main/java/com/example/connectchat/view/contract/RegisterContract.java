package com.example.connectchat.view.contract;

public interface RegisterContract {

    interface View {
        void showLoading();
        void hideLoading();
        void onRegisterSuccess();
        void onRegisterFailed(String message);
    }

    interface Presenter {
        void register(String username, String password, String confirmPassword);
        void destroy();
    }
}
