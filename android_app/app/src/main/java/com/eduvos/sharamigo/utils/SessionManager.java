package com.eduvos.sharamigo.utils;

import android.content.Context;
import android.content.SharedPreferences;

// Keeps the login token and basic user info on the phone (private to this app).
public class SessionManager {
    private static final String PREFS = "sharamigo_session";
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static SessionManager get(Context context) {
        return new SessionManager(context);
    }

    public void save(String token, int userId, String name) {
        prefs.edit().putString("token", token).putInt("user_id", userId).putString("name", name).apply();
    }

    public String getToken() { return prefs.getString("token", null); }
    public int getUserId() { return prefs.getInt("user_id", 0); }
    public String getName() { return prefs.getString("name", ""); }
    public boolean isLoggedIn() { return getToken() != null && getUserId() > 0; }
    public void clear() { prefs.edit().clear().apply(); }
}
