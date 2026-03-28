package com.rogger.bp.notification;

import android.content.Context;
import android.content.SharedPreferences;

public class NotificationPrefs {
    private static final String PREF = "notif_prefs";
    private static final String DAYS = "days_alert";
    private static final String ALERT = "alert";
    private static final String HOUR = "notif_hour";
    private static final String MINUTE = "notif_minute";

    public static void onAlert(Context context, boolean beep) {
        SharedPreferences sharedp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedp.edit();
        editor.putBoolean(ALERT,beep);
        editor.apply();
    }
    public static void saveDays(Context c, int days) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putInt(DAYS, days)
                .apply();
    }

    public static int getDays(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getInt(DAYS, 3); // padrão 3 dias
    }

    public static void saveTime(Context c, int hour, int minute) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putInt(HOUR, hour)
                .putInt(MINUTE, minute)
                .apply();
    }

    public static int getHour(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getInt(HOUR, 9); // padrão 09:00
    }

    public static int getMinute(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getInt(MINUTE, 0); // padrão :00
    }

    public static boolean getAlert(Context context) {
        SharedPreferences sharedPre = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sharedPre.getBoolean(ALERT, false);
    }
}
