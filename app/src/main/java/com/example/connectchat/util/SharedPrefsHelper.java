package com.example.connectchat.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsHelper {

    private static final String PREF_NAME      = "connect_chat_prefs";
    private static final String KEY_USERNAME   = "logged_in_username";
    private static final String KEY_AVATAR_PATH = "avatar_file_path";

    private final SharedPreferences prefs;

    public SharedPrefsHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public boolean isLoggedIn() {
        return getUsername() != null;
    }

    public void logout() {
        prefs.edit().remove(KEY_USERNAME).remove(KEY_AVATAR_PATH).apply();
    }

    /** Save the absolute path of the locally-stored profile picture. */
    public void saveAvatarPath(String path) {
        prefs.edit().putString(KEY_AVATAR_PATH, path).apply();
    }

    /** Returns the saved avatar file path, or null if none set. */
    public String getAvatarPath() {
        return prefs.getString(KEY_AVATAR_PATH, null);
    }
}
