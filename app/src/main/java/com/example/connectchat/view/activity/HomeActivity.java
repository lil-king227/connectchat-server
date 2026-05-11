package com.example.connectchat.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.connectchat.databinding.ActivityHomeBinding;
import com.example.connectchat.model.db.entity.Conversation;
import com.example.connectchat.model.network.WebSocketManager;
import com.example.connectchat.presenter.HomePresenter;
import com.example.connectchat.util.SharedPrefsHelper;
import com.example.connectchat.view.adapter.ConversationAdapter;
import com.example.connectchat.view.contract.HomeContract;

import java.util.List;

public class HomeActivity extends AppCompatActivity implements HomeContract.View {

    private ActivityHomeBinding binding;
    private HomePresenter presenter;
    private SharedPrefsHelper prefs;
    private ConversationAdapter adapter;
    private String currentUsername;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = new SharedPrefsHelper(this);
        currentUsername = prefs.getUsername();
        presenter = new HomePresenter(this, this);

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitle("ConnectChat (" + currentUsername + ")");

        // Set up RecyclerView
        adapter = new ConversationAdapter(currentUsername, conversation -> {
            String peer = conversation.participantOne.equals(currentUsername)
                    ? conversation.participantTwo : conversation.participantOne;
            openChat(peer);
        });
        binding.rvConversations.setLayoutManager(new LinearLayoutManager(this));
        binding.rvConversations.setAdapter(adapter);

        // FAB - start new chat
        binding.fabNewChat.setOnClickListener(v -> showNewChatDialog());

        // Connect WebSocket
        WebSocketManager.getInstance().connect(currentUsername, new WebSocketManager.MessageListener() {
            @Override
            public void onTextMessageReceived(String fromUser, String content, long timestamp) {
                // Refresh conversation list when new message arrives
                presenter.loadConversations(currentUsername);
            }
            @Override
            public void onImageMessageReceived(String fromUser, String imagePath, long timestamp) {
                presenter.loadConversations(currentUsername);
            }
            @Override
            public void onConnected() { /* handled in ChatActivity */ }
            @Override
            public void onDisconnected() { /* handled in ChatActivity */ }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.loadConversations(currentUsername);
    }

    private void showNewChatDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter username");
        new AlertDialog.Builder(this)
                .setTitle("New Chat")
                .setView(input)
                .setPositiveButton("Start", (dialog, which) -> {
                    String peer = input.getText().toString().trim();
                    if (peer.equalsIgnoreCase(currentUsername)) {
                        Toast.makeText(this, "You can't chat with yourself!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    presenter.startConversation(currentUsername, peer);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openChat(String peerUsername) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_PEER_USERNAME, peerUsername);
        startActivity(intent);
    }

    @Override
    public void showConversations(List<Conversation> conversations) {
        mainHandler.post(() -> {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.rvConversations.setVisibility(View.VISIBLE);
            adapter.setConversations(conversations);
        });
    }

    @Override
    public void showEmpty() {
        mainHandler.post(() -> {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.rvConversations.setVisibility(View.GONE);
        });
    }

    @Override
    public void onNewConversationStarted(String peerUsername) {
        mainHandler.post(() -> openChat(peerUsername));
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(com.example.connectchat.R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == com.example.connectchat.R.id.action_logout) {
            prefs.logout();
            WebSocketManager.getInstance().disconnect();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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
