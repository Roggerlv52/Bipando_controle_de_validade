package com.rogger.bp.notification;

import android.content.Context;
import android.content.SharedPreferences;

public class NotificationPrefs {
    private static final String PREF = "notif_prefs";
    private static final String DAYS = "days_alert";
    private static final String ALERT = "alert";

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
    public static boolean getAlert(Context context) {
        SharedPreferences sharedPre = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sharedPre.getBoolean(ALERT, false);
    }
}
