package com.example.connectchat.model.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "users")
public class User {

    @PrimaryKey
    @NonNull
    public String username;

    public String password;
    public long createTime;

    public User(@NonNull String username, String password, long createTime) {
        this.username = username;
        this.password = password;
        this.createTime = createTime;
    }
}
