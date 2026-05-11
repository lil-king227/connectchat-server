package com.example.connectchat.model.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.connectchat.model.db.entity.Message;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    long insert(Message message);

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    List<Message> getMessagesForConversation(String conversationId);

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    void deleteByConversation(String conversationId);

    @Query("DELETE FROM messages")
    void deleteAll();
}
