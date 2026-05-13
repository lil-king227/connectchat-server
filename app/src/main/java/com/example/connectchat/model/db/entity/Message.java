package com.example.connectchat.model.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {

    public static final String TYPE_TEXT    = "text";
    public static final String TYPE_IMAGE   = "image";
    public static final String TYPE_DELETED = "deleted";   // message deleted for everyone

    public static final String STATUS_SENDING  = "sending";
    public static final String STATUS_SENT     = "sent";
    public static final String STATUS_RECEIVED = "received";
    public static final String STATUS_READ     = "read";   // sender knows it was read

    @PrimaryKey(autoGenerate = true)
    public long messageId;

    public String conversationId;
    public String senderId;
    public String receiverId;
    public String content;      // text content or image URI/path
    public String type;         // "text" or "image"
    public String status;
    public long timestamp;

    public Message(String conversationId, String senderId, String receiverId,
                   String content, String type, String status, long timestamp) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.type = type;
        this.status = status;
        this.timestamp = timestamp;
    }
}
