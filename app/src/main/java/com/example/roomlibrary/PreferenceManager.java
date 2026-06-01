package com.example.roomlibrary;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "ExpenseTrackerPrefs";
    private static final String KEY_CURRENCY_SYMBOL = "currency_symbol";
    private static final String DEFAULT_SYMBOL = "₹";
    
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_LIMIT_EXCEEDS_NOTIF = "limit_exceeds_notif";
    private static final String KEY_DAILY_ALERT_NOTIF = "daily_alert_notif";
    private static final String KEY_DAILY_ALERT_TIME = "daily_alert_time";

    private SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setCurrencySymbol(String symbol) {
        sharedPreferences.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply();
    }

    public String getCurrencySymbol() {
        return sharedPreferences.getString(KEY_CURRENCY_SYMBOL, DEFAULT_SYMBOL);
    }

    public void setUserName(String name) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "User");
    }

    public void setLimitExceedsNotif(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_LIMIT_EXCEEDS_NOTIF, enabled).apply();
    }

    public boolean isLimitExceedsNotifEnabled() {
        return sharedPreferences.getBoolean(KEY_LIMIT_EXCEEDS_NOTIF, true);
    }

    public void setDailyAlertNotif(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DAILY_ALERT_NOTIF, enabled).apply();
    }

    public boolean isDailyAlertNotifEnabled() {
        return sharedPreferences.getBoolean(KEY_DAILY_ALERT_NOTIF, false);
    }

    public void setDailyAlertTime(String time) {
        sharedPreferences.edit().putString(KEY_DAILY_ALERT_TIME, time).apply();
    }

    public String getDailyAlertTime() {
        return sharedPreferences.getString(KEY_DAILY_ALERT_TIME, "20:00");
    }
}
