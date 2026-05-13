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

    /** Mark a specific message as deleted (type → "deleted", content cleared). */
    @Query("UPDATE messages SET type = 'deleted', content = '' " +
           "WHERE senderId = :senderId AND conversationId = :conversationId AND timestamp = :timestamp")
    void markAsDeleted(String senderId, String conversationId, long timestamp);

    /** Mark all sent messages up to a timestamp as read (for read receipts). */
    @Query("UPDATE messages SET status = 'read' " +
           "WHERE conversationId = :conversationId AND senderId = :senderId AND timestamp <= :upToTimestamp")
    void markMessagesRead(String conversationId, String senderId, long upToTimestamp);

    /** Full-text search across all conversations (excludes deleted messages). */
    @Query("SELECT * FROM messages WHERE type != 'deleted' AND content LIKE '%' || :query || '%' " +
           "ORDER BY timestamp DESC LIMIT 100")
    List<Message> searchMessages(String query);
}
