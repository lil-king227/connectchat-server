package com.example.connectchat.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connectchat.R;
import com.example.connectchat.model.db.entity.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT     = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final String currentUsername;
    private final List<Message> messages = new ArrayList<>();
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MessageAdapter(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public void setMessages(List<Message> msgs) {
        messages.clear();
        messages.addAll(msgs);
        notifyDataSetChanged();
    }

    public void addMessage(Message msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).senderId.equals(currentUsername)
                ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            View v = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new MessageViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_message_received, parent, false);
            return new MessageViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        MessageViewHolder vh = (MessageViewHolder) holder;

        String time = timeFormat.format(new Date(msg.timestamp));
        vh.tvTime.setText(time);

        if (Message.TYPE_IMAGE.equals(msg.type)) {
            vh.tvMessage.setVisibility(View.GONE);
            vh.ivImage.setVisibility(View.VISIBLE);
            Glide.with(vh.ivImage.getContext())
                    .load(msg.content)
                    .centerCrop()
                    .into(vh.ivImage);
        } else {
            vh.tvMessage.setVisibility(View.VISIBLE);
            vh.ivImage.setVisibility(View.GONE);
            vh.tvMessage.setText(msg.content);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTime;
        ImageView ivImage;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime    = itemView.findViewById(R.id.tvTime);
            ivImage   = itemView.findViewById(R.id.ivImage);
        }
    }
}
