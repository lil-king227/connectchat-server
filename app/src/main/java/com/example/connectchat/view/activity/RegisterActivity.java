package com.example.connectchat.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.connectchat.databinding.ActivityRegisterBinding;
import com.example.connectchat.presenter.RegisterPresenter;
import com.example.connectchat.view.contract.RegisterContract;

public class RegisterActivity extends AppCompatActivity implements RegisterContract.View {

    private ActivityRegisterBinding binding;
    private RegisterPresenter presenter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        presenter = new RegisterPresenter(this, this);

        binding.btnRegister.setOnClickListener(v -> {
            String username = binding.etUsername.getText().toString();
            String password = binding.etPassword.getText().toString();
            String confirm  = binding.etConfirmPassword.getText().toString();
            presenter.register(username, password, confirm);
        });

        binding.tvGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    public void showLoading() {
        mainHandler.post(() -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnRegister.setEnabled(false);
        });
    }

    @Override
    public void hideLoading() {
        mainHandler.post(() -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnRegister.setEnabled(true);
        });
    }

    @Override
    public void onRegisterSuccess() {
        mainHandler.post(() -> {
            hideLoading();
            Toast.makeText(this, "Account created! Please login.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    public void onRegisterFailed(String message) {
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
