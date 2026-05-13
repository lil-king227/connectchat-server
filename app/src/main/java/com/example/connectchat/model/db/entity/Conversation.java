package com.example.connectchat.model.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "conversations")
public class Conversation {

    @PrimaryKey
    @NonNull
    public String conversationId; // e.g. "alice_bob" (sorted alphabetically)

    public String participantOne;
    public String participantTwo;
    public String lastMessage;
    public long lastMessageTime;
    public int unreadCount;

    public Conversation(@NonNull String conversationId, String participantOne,
                        String participantTwo, String lastMessage,
                        long lastMessageTime, int unreadCount) {
        this.conversationId = conversationId;
        this.participantOne = participantOne;
        this.participantTwo = participantTwo;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;
    }

    // Helper: build a stable conversation ID from two usernames (case-insensitive)
    public static String buildId(String userA, String userB) {
        String a = userA.toLowerCase();
        String b = userB.toLowerCase();
        if (a.compareTo(b) <= 0) {
            return a + "_" + b;
        } else {
            return b + "_" + a;
        }
    }
}
