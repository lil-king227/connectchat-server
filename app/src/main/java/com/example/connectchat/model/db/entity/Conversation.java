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

    // Helper: build a stable conversation ID from two usernames
    public static String buildId(String userA, String userB) {
        if (userA.compareTo(userB) <= 0) {
            return userA + "_" + userB;
        } else {
            return userB + "_" + userA;
        }
    }
}
