package com.example.connectchat.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectchat.R;
import com.example.connectchat.model.db.entity.Conversation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConvViewHolder> {

    public interface OnConversationClickListener {
        void onClick(Conversation conversation);
    }

    private final String currentUsername;
    private final List<Conversation> conversations = new ArrayList<>();
    private final OnConversationClickListener listener;
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ConversationAdapter(String currentUsername, OnConversationClickListener listener) {
        this.currentUsername = currentUsername;
        this.listener = listener;
    }

    public void setConversations(List<Conversation> convs) {
        conversations.clear();
        conversations.addAll(convs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConvViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ConvViewHolder holder, int position) {
        Conversation conv = conversations.get(position);

        // Determine peer name
        String peer = conv.participantOne.equals(currentUsername)
                ? conv.participantTwo : conv.participantOne;

        holder.tvPeerName.setText(peer);
        holder.tvAvatar.setText(peer.substring(0, 1).toUpperCase());
        holder.tvLastMessage.setText(conv.lastMessage.isEmpty() ? "Say hi!" : conv.lastMessage);

        if (conv.lastMessageTime > 0) {
            holder.tvTime.setText(timeFormat.format(new Date(conv.lastMessageTime)));
        }

        if (conv.unreadCount > 0) {
            holder.tvUnread.setVisibility(View.VISIBLE);
            holder.tvUnread.setText(String.valueOf(conv.unreadCount));
        } else {
            holder.tvUnread.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(conv));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ConvViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvPeerName, tvLastMessage, tvTime, tvUnread;

        ConvViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar      = itemView.findViewById(R.id.tvAvatar);
            tvPeerName    = itemView.findViewById(R.id.tvPeerName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime        = itemView.findViewById(R.id.tvTime);
            tvUnread      = itemView.findViewById(R.id.tvUnread);
        }
    }
}
