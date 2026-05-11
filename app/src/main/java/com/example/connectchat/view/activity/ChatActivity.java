package com.example.connectchat.view.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.connectchat.databinding.ActivityChatBinding;
import com.example.connectchat.model.db.entity.Conversation;
import com.example.connectchat.model.db.entity.Message;
import com.example.connectchat.model.network.WebSocketManager;
import com.example.connectchat.presenter.ChatPresenter;
import com.example.connectchat.util.SharedPrefsHelper;
import com.example.connectchat.view.adapter.MessageAdapter;
import com.example.connectchat.view.contract.ChatContract;

import java.util.List;

public class ChatActivity extends AppCompatActivity implements ChatContract.View {

    public static final String EXTRA_PEER_USERNAME = "peer_username";

    private ActivityChatBinding binding;
    private ChatPresenter presenter;
    private MessageAdapter adapter;
    private String currentUsername;
    private String peerUsername;
    private String conversationId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Image picker launcher
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    presenter.sendImageMessage(conversationId, currentUsername,
                            peerUsername, uri.toString());
                }
            });

    // Permission launcher
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openImagePicker();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPrefsHelper prefs = new SharedPrefsHelper(this);
        currentUsername = prefs.getUsername();
        peerUsername = getIntent().getStringExtra(EXTRA_PEER_USERNAME);
        conversationId = Conversation.buildId(currentUsername, peerUsername);

        presenter = new ChatPresenter(this, this, currentUsername, peerUsername);

        // Toolbar
        setSupportActionBar(binding.toolbar);
        binding.tvPeerName.setText(peerUsername);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Connection status
        boolean connected = WebSocketManager.getInstance().isConnected();
        showConnectionStatus(connected);

        // RecyclerView
        adapter = new MessageAdapter(currentUsername);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);

        // Send button
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString();
            presenter.sendTextMessage(conversationId, currentUsername, peerUsername, text);
            binding.etMessage.setText("");
        });

        // Image button
        binding.btnPickImage.setOnClickListener(v -> checkPermissionAndPickImage());

        // Load history
        presenter.loadHistory(conversationId);
    }

    private void checkPermissionAndPickImage() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    @Override
    public void showMessages(List<Message> messages) {
        mainHandler.post(() -> {
            adapter.setMessages(messages);
            if (!messages.isEmpty()) {
                binding.rvMessages.scrollToPosition(messages.size() - 1);
            }
        });
    }

    @Override
    public void appendMessage(Message message) {
        mainHandler.post(() -> {
            adapter.addMessage(message);
            binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        });
    }

    @Override
    public void showConnectionStatus(boolean connected) {
        mainHandler.post(() -> {
            binding.tvStatus.setText(connected ? "Online" : "Connecting…");
            binding.tvStatus.setTextColor(connected
                    ? getColor(com.example.connectchat.R.color.accent)
                    : getColor(android.R.color.darker_gray));
        });
    }

    @Override
    public void onImageSent(Message message) {
        mainHandler.post(() -> {
            adapter.addMessage(message);
            binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(com.example.connectchat.R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == com.example.connectchat.R.id.action_clear_history) {
            new AlertDialog.Builder(this)
                    .setTitle("Clear History")
                    .setMessage("Delete all messages in this chat?")
                    .setPositiveButton("Clear", (d, w) ->
                            presenter.clearHistory(conversationId))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }
}
