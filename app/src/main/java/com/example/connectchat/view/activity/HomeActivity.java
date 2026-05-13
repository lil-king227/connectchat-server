package com.example.connectchat.view.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.connectchat.databinding.ActivityHomeBinding;
import com.example.connectchat.model.db.AppDatabase;
import com.example.connectchat.model.db.entity.Conversation;
import com.example.connectchat.model.db.entity.Message;
import com.example.connectchat.model.network.WebSocketManager;
import com.example.connectchat.presenter.HomePresenter;
import com.example.connectchat.util.NotificationHelper;
import com.example.connectchat.util.SharedPrefsHelper;
import com.example.connectchat.view.adapter.ConversationAdapter;
import com.example.connectchat.view.contract.HomeContract;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity implements HomeContract.View {

    private ActivityHomeBinding binding;
    private HomePresenter presenter;
    private SharedPrefsHelper prefs;
    private ConversationAdapter adapter;
    private String currentUsername;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Tracks which usernames are currently online. */
    private final Set<String> onlineUsers = new HashSet<>();

    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    // Persistent listener — saves incoming messages to DB even from home screen
    private final WebSocketManager.MessageListener homeListener = new WebSocketManager.MessageListener() {

        @Override
        public void onTextMessageReceived(String fromUser, String content, long timestamp) {
            saveIncomingMessage(fromUser, content, Message.TYPE_TEXT, timestamp);
        }

        @Override
        public void onImageMessageReceived(String fromUser, String imagePath, long timestamp) {
            saveIncomingMessage(fromUser, imagePath, Message.TYPE_IMAGE, timestamp);
        }

        @Override
        public void onConnected() {}

        @Override
        public void onDisconnected() {}

        // --- Phase 1: presence ---

        @Override
        public void onUserOnline(String username) {
            mainHandler.post(() -> {
                onlineUsers.add(username);
                adapter.setOnlineUsers(new HashSet<>(onlineUsers));
            });
        }

        @Override
        public void onUserOffline(String username, long lastSeen) {
            mainHandler.post(() -> {
                onlineUsers.remove(username);
                adapter.setOnlineUsers(new HashSet<>(onlineUsers));
            });
        }

        @Override
        public void onOnlineListReceived(List<String> users) {
            mainHandler.post(() -> {
                onlineUsers.clear();
                onlineUsers.addAll(users);
                adapter.setOnlineUsers(new HashSet<>(onlineUsers));
            });
        }

        // --- Phase 1: delete for everyone ---
        // Handle delete events that arrive while user is NOT inside ChatActivity.
        // We update the DB so the message shows as deleted when they open the chat.
        @Override
        public void onMessageDeleteReceived(String fromUser, String conversationId, long timestamp) {
            executor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(HomeActivity.this);
                db.messageDao().markAsDeleted(fromUser, conversationId, timestamp);
            });
        }
    };

    private void saveIncomingMessage(String fromUser, String content, String type, long timestamp) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(HomeActivity.this);
            String convId = Conversation.buildId(currentUsername, fromUser);

            Conversation conv = db.conversationDao().findById(convId);
            if (conv == null) {
                conv = new Conversation(convId, currentUsername, fromUser,
                        content, timestamp, 1);
                db.conversationDao().insert(conv);
            } else {
                conv.lastMessage = content;
                conv.lastMessageTime = timestamp;
                conv.unreadCount++;
                db.conversationDao().update(conv);
            }

            Message msg = new Message(convId, fromUser, currentUsername,
                    content, type, Message.STATUS_RECEIVED, timestamp);
            db.messageDao().insert(msg);

            if (!fromUser.equalsIgnoreCase(ChatActivity.activePeer)) {
                String notifText = Message.TYPE_IMAGE.equals(type) ? "📷 Image" : content;
                NotificationHelper.showMessageNotification(HomeActivity.this, fromUser, notifText);
            }

            presenter.loadConversations(currentUsername);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NotificationHelper.createChannel(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        prefs = new SharedPrefsHelper(this);
        currentUsername = prefs.getUsername();
        presenter = new HomePresenter(this, this);

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitle("ConnectChat (" + currentUsername + ")");

        adapter = new ConversationAdapter(currentUsername, conversation -> {
            String peer = conversation.participantOne.equals(currentUsername)
                    ? conversation.participantTwo : conversation.participantOne;
            openChat(peer);
        });
        binding.rvConversations.setLayoutManager(new LinearLayoutManager(this));
        binding.rvConversations.setAdapter(adapter);

        binding.fabNewChat.setOnClickListener(v -> showNewChatDialog());

        WebSocketManager.getInstance().connect(currentUsername, homeListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebSocketManager wsm = WebSocketManager.getInstance();
        wsm.setListener(homeListener);
        // Refresh online-user list every time we return to this screen
        if (wsm.isConnected()) {
            wsm.requestUserList();
        }
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

    // -----------------------------------------------------------------------
    // HomeContract.View
    // -----------------------------------------------------------------------

    @Override
    public void showConversations(List<Conversation> conversations) {
        mainHandler.post(() -> {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.rvConversations.setVisibility(View.VISIBLE);
            adapter.setConversations(conversations);
            adapter.setOnlineUsers(new HashSet<>(onlineUsers));
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

    // -----------------------------------------------------------------------
    // Menu
    // -----------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(com.example.connectchat.R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == com.example.connectchat.R.id.action_search_users) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        } else if (id == com.example.connectchat.R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == com.example.connectchat.R.id.action_logout) {
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
        executor.shutdown();
    }
}
