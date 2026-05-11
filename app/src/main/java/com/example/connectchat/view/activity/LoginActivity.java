package com.example.connectchat.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.connectchat.databinding.ActivityLoginBinding;
import com.example.connectchat.presenter.LoginPresenter;
import com.example.connectchat.util.SharedPrefsHelper;
import com.example.connectchat.view.contract.LoginContract;

public class LoginActivity extends AppCompatActivity implements LoginContract.View {

    private ActivityLoginBinding binding;
    private LoginPresenter presenter;
    private SharedPrefsHelper prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = new SharedPrefsHelper(this);
        presenter = new LoginPresenter(this, this);

        binding.btnLogin.setOnClickListener(v -> {
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString();
            presenter.login(username, password);
        });

        binding.tvGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    @Override
    public void showLoading() {
        mainHandler.post(() -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnLogin.setEnabled(false);
        });
    }

    @Override
    public void hideLoading() {
        mainHandler.post(() -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnLogin.setEnabled(true);
        });
    }

    @Override
    public void onLoginSuccess(String username) {
        mainHandler.post(() -> {
            hideLoading();
            prefs.saveUsername(username);
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onLoginFailed(String message) {
        mainHandler.post(() -> {
            hideLoading();
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }
}
