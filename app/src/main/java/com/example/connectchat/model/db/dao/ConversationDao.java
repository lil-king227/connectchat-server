package com.example.connectchat.model.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.connectchat.model.db.entity.Conversation;

import java.util.List;

@Dao
public interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Conversation conversation);

    @Update
    void update(Conversation conversation);

    @Query("SELECT * FROM conversations WHERE participantOne = :username OR participantTwo = :username ORDER BY lastMessageTime DESC")
    List<Conversation> getConversationsForUser(String username);

    @Query("SELECT * FROM conversations WHERE conversationId = :id LIMIT 1")
    Conversation findById(String id);

    @Query("UPDATE conversations SET unreadCount = 0 WHERE conversationId = :id")
    void markAsRead(String id);

    @Query("DELETE FROM conversations WHERE conversationId = :id")
    void deleteById(String id);
}
