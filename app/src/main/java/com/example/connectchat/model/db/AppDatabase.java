package com.example.connectchat.model.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.connectchat.model.db.dao.ConversationDao;
import com.example.connectchat.model.db.dao.MessageDao;
import com.example.connectchat.model.db.dao.UserDao;
import com.example.connectchat.model.db.entity.Conversation;
import com.example.connectchat.model.db.entity.Message;
import com.example.connectchat.model.db.entity.User;

@Database(entities = {User.class, Message.class, Conversation.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract UserDao userDao();
    public abstract MessageDao messageDao();
    public abstract ConversationDao conversationDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "connectchat_db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
