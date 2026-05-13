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

    /** Callback so ChatActivity can show the delete dialog. */
    public interface OnMessageLongClickListener {
        void onLongClick(Message message);
    }

    private final String currentUsername;
    private final List<Message> messages = new ArrayList<>();
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());
    private OnMessageLongClickListener longClickListener;

    public MessageAdapter(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener l) {
        this.longClickListener = l;
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

    /** Mark a message as deleted in-place and refresh its row. */
    public void markDeleted(String senderId, long timestamp) {
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (m.senderId.equals(senderId) && m.timestamp == timestamp) {
                m.type = Message.TYPE_DELETED;
                m.content = "";
                notifyItemChanged(i);
                return;
            }
        }
    }

    /** Update all sent messages up to upToTimestamp to STATUS_READ. */
    public void markReadUpTo(String senderId, long upToTimestamp) {
        boolean changed = false;
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (m.senderId.equals(senderId) && m.timestamp <= upToTimestamp
                    && !Message.STATUS_READ.equals(m.status)) {
                m.status = Message.STATUS_READ;
                notifyItemChanged(i);
                changed = true;
            }
        }
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
            return new SentViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        String time = timeFormat.format(new Date(msg.timestamp));

        if (holder instanceof SentViewHolder) {
            bindSent((SentViewHolder) holder, msg, time);
        } else {
            bindReceived((ReceivedViewHolder) holder, msg, time);
        }
    }

    private void bindSent(SentViewHolder vh, Message msg, String time) {
        vh.tvTime.setText(time);

        if (Message.TYPE_DELETED.equals(msg.type)) {
            // Show deleted placeholder
            vh.tvDeleted.setVisibility(View.VISIBLE);
            vh.tvMessage.setVisibility(View.GONE);
            vh.ivImage.setVisibility(View.GONE);
            vh.tvTick.setVisibility(View.GONE);
            vh.itemView.setOnLongClickListener(null);
            return;
        }

        vh.tvDeleted.setVisibility(View.GONE);

        // Content
        if (Message.TYPE_IMAGE.equals(msg.type)) {
            vh.tvMessage.setVisibility(View.GONE);
            vh.ivImage.setVisibility(View.VISIBLE);
            Glide.with(vh.ivImage.getContext()).load(msg.content).centerCrop().into(vh.ivImage);
        } else {
            vh.tvMessage.setVisibility(View.VISIBLE);
            vh.ivImage.setVisibility(View.GONE);
            vh.tvMessage.setText(msg.content);
        }

        // Tick text: grey ✓ = delivered, green ✓✓ = read
        vh.tvTick.setVisibility(View.VISIBLE);
        if (Message.STATUS_READ.equals(msg.status)) {
            vh.tvTick.setText("✓✓");
            vh.tvTick.setTextColor(
                    vh.itemView.getContext().getColor(com.example.connectchat.R.color.accent));
        } else {
            vh.tvTick.setText("✓");
            vh.tvTick.setTextColor(
                    vh.itemView.getContext().getColor(com.example.connectchat.R.color.time_text));
        }

        // Long-press → delete for everyone
        vh.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onLongClick(msg);
            return true;
        });
    }

    private void bindReceived(ReceivedViewHolder vh, Message msg, String time) {
        vh.tvTime.setText(time);

        if (Message.TYPE_DELETED.equals(msg.type)) {
            vh.tvDeleted.setVisibility(View.VISIBLE);
            vh.tvMessage.setVisibility(View.GONE);
            vh.ivImage.setVisibility(View.GONE);
            return;
        }

        vh.tvDeleted.setVisibility(View.GONE);

        if (Message.TYPE_IMAGE.equals(msg.type)) {
            vh.tvMessage.setVisibility(View.GONE);
            vh.ivImage.setVisibility(View.VISIBLE);
            Glide.with(vh.ivImage.getContext()).load(msg.content).centerCrop().into(vh.ivImage);
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

    // -----------------------------------------------------------------------
    // ViewHolders
    // -----------------------------------------------------------------------

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvDeleted, tvTick;
        ImageView ivImage;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime    = itemView.findViewById(R.id.tvTime);
            tvDeleted = itemView.findViewById(R.id.tvDeleted);
            ivImage   = itemView.findViewById(R.id.ivImage);
            tvTick    = itemView.findViewById(R.id.tvTick);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvDeleted;
        ImageView ivImage;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime    = itemView.findViewById(R.id.tvTime);
            tvDeleted = itemView.findViewById(R.id.tvDeleted);
            ivImage   = itemView.findViewById(R.id.ivImage);
        }
    }
}
