package com.example.connectchat.view.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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

    public static volatile String activePeer = null;

    private ActivityChatBinding binding;
    private ChatPresenter presenter;
    private MessageAdapter adapter;
    private String currentUsername;
    private String peerUsername;
    private String conversationId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Track latest received-message timestamp so we can send a read receipt
    private long latestReceivedTimestamp = 0;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    presenter.sendImageMessage(conversationId, currentUsername,
                            peerUsername, uri.toString());
                }
            });

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openImagePicker();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);

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

        // RecyclerView
        adapter = new MessageAdapter(currentUsername);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);

        // Long-press on a sent message → offer delete
        adapter.setOnMessageLongClickListener(message -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete message")
                    .setMessage("Delete this message for everyone?")
                    .setPositiveButton("Delete for everyone", (d, w) ->
                            presenter.deleteMessage(message))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Send button
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString();
            presenter.sendTextMessage(conversationId, currentUsername, peerUsername, text);
            binding.etMessage.setText("");
        });

        // Image button
        binding.btnPickImage.setOnClickListener(v -> checkPermissionAndPickImage());

        // Typing indicator — send event on each keystroke (debounced by server)
        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) presenter.sendTypingEvent();
            }
        });

        // Load history
        presenter.loadHistory(conversationId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activePeer = peerUsername;
        presenter.reattachListener();
        showConnectionStatus(WebSocketManager.getInstance().isConnected());
    }

    @Override
    protected void onPause() {
        super.onPause();
        activePeer = null;
        // Send read receipt for everything we've seen
        if (latestReceivedTimestamp > 0) {
            presenter.sendReadReceipts(conversationId, peerUsername, latestReceivedTimestamp);
        }
    }

    // -----------------------------------------------------------------------
    // ChatContract.View
    // -----------------------------------------------------------------------

    @Override
    public void showMessages(List<Message> messages) {
        adapter.setMessages(messages);
        if (!messages.isEmpty()) {
            binding.rvMessages.scrollToPosition(messages.size() - 1);
        }
        // Find the latest timestamp among received messages to send a read receipt
        for (Message m : messages) {
            if (!m.senderId.equals(currentUsername) && m.timestamp > latestReceivedTimestamp) {
                latestReceivedTimestamp = m.timestamp;
            }
        }
        if (latestReceivedTimestamp > 0) {
            presenter.sendReadReceipts(conversationId, peerUsername, latestReceivedTimestamp);
        }
    }

    @Override
    public void appendMessage(Message message) {
        adapter.addMessage(message);
        binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        // Track latest received timestamp for auto read receipt
        if (!message.senderId.equals(currentUsername) && message.timestamp > latestReceivedTimestamp) {
            latestReceivedTimestamp = message.timestamp;
            presenter.sendReadReceipts(conversationId, peerUsername, latestReceivedTimestamp);
        }
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
        adapter.addMessage(message);
        binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
    }

    @Override
    public void showTypingIndicator(boolean visible) {
        binding.tvTypingIndicator.setText(peerUsername + " is typing…");
        binding.tvTypingIndicator.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRemoteMessageDeleted(String senderId, long timestamp) {
        adapter.markDeleted(senderId, timestamp);
    }

    @Override
    public void onReadReceiptUpdated(String conversationId, long upToTimestamp) {
        adapter.markReadUpTo(currentUsername, upToTimestamp);
    }

    @Override
    public void onConversationDeleted() {
        finish();
    }

    // -----------------------------------------------------------------------
    // Menu
    // -----------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(com.example.connectchat.R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == com.example.connectchat.R.id.action_clear_history) {
            new AlertDialog.Builder(this)
                    .setTitle("Clear History")
                    .setMessage("Delete all messages in this chat?")
                    .setPositiveButton("Clear", (d, w) ->
                            presenter.clearHistory(conversationId))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        } else if (id == com.example.connectchat.R.id.action_delete_conversation) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Conversation")
                    .setMessage("Remove this conversation and all messages?")
                    .setPositiveButton("Delete", (d, w) ->
                            presenter.deleteConversation(conversationId))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

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
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }
}
