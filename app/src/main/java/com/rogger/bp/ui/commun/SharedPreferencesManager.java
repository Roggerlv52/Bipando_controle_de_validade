package com.rogger.bp.ui.commun;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SharedPreferencesManager {
    private static final String TAG = "SharedPreferencesManager";
    private static final String OLD_PREF_NAME = "shared_key_date";
    private static final String SECURE_PREF_NAME = "secure_shared_key_date";

    private static volatile SharedPreferences sSharedPreferences;

    private static synchronized SharedPreferences getPreferences(Context context) {
        if (sSharedPreferences != null) {
            return sSharedPreferences;
        }

        Context appContext = context.getApplicationContext();
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences encryptedPrefs = EncryptedSharedPreferences.create(
                    appContext,
                    SECURE_PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            migrateOldPrefsIfNeeded(appContext, encryptedPrefs);
            sSharedPreferences = encryptedPrefs;
            return sSharedPreferences;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing EncryptedSharedPreferences, falling back to standard: " + e.getMessage(), e);
            sSharedPreferences = appContext.getSharedPreferences(OLD_PREF_NAME, Context.MODE_PRIVATE);
            return sSharedPreferences;
        }
    }

    private static void migrateOldPrefsIfNeeded(Context context, SharedPreferences encryptedPrefs) {
        SharedPreferences oldPrefs = context.getSharedPreferences(OLD_PREF_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = oldPrefs.getAll();
        if (all != null && !all.isEmpty()) {
            Log.d(TAG, "Migrating " + all.size() + " unencrypted preferences to EncryptedSharedPreferences...");
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                }
            }
            editor.apply();
            oldPrefs.edit().clear().apply();
            Log.d(TAG, "Migration to EncryptedSharedPreferences completed successfully.");
        }
    }

    // ✅ Guarda se o utilizador é Premium
    public static void setPremiumState(Context context, boolean isPremium) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean("is_premium", isPremium);
        editor.apply();
    }

    // ✅ Retorna se o utilizador é Premium
    public static boolean isPremium(Context context) {
        return getPreferences(context).getBoolean("is_premium", false);
    }

    // Método para adicionar um valor ao SharedPreferences
    public static void sharedBeepState(Context context, String key, boolean beep) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean(key, beep);
        editor.apply();
    }

    // Método para atualizar um valor do SharedPreferences
    public static void updateThemeNumber(Context context, String key, int newThemeNumber) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt(key, newThemeNumber);
        editor.apply();
    }

    // Método para recuperar um valor do SharedPreferences
    public static int getThemeNumber(Context context, String key) {
        return getPreferences(context).getInt(key, 0);
    }

    public static boolean getBeepState(Context context, String key) {
        return getPreferences(context).getBoolean(key, false);
    }

    public static void setLoginState(Context context, String key, boolean state) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean(key, state);
        editor.apply();
    }

    public static boolean getLoginState(Context context, String key) {
        return getPreferences(context).getBoolean(key, false);
    }

    public static void saveUserInfo(Context context, String uid, String name, String imageUrl, String email) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString("userUid", uid);
        editor.putString("nameUser", name);
        editor.putString("profile_image_url", imageUrl);
        editor.putString("email_", email);
        editor.apply();
    }

    public static List<String> getUserInfo(Context context) {
        SharedPreferences sharedPreferences = getPreferences(context);
        List<String> userInfo = new ArrayList<>();
        userInfo.add(sharedPreferences.getString("userUid", null));
        userInfo.add(sharedPreferences.getString("nameUser", "Nome não encontrado"));
        userInfo.add(sharedPreferences.getString("profile_image_url", null));
        userInfo.add(sharedPreferences.getString("email_", "No email"));
        return userInfo;
    }

    public static void clearUserInfo(Context context) {
        int savedWorkMode = getWorkMode(context);
        int savedTheme = getThemeNumber(context, "chave");
        boolean savedBeep = getBeepState(context, "beep");
        int savedDatePicker = getDatePickerType(context);
        boolean savedTour = isTourCompleted(context);

        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.clear(); // remove tudo
        editor.apply();

        // Restaura as preferências que devem persistir
        setWorkMode(context, savedWorkMode);
        updateThemeNumber(context, "chave", savedTheme);
        sharedBeepState(context, "beep", savedBeep);
        setDatePickerType(context, savedDatePicker);
        setTourCompleted(context, savedTour);
    }

    public static void setDatePickerType(Context context, int type) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt("date_picker_type", type);
        editor.apply();
    }

    public static int getDatePickerType(Context context) {
        return getPreferences(context).getInt("date_picker_type", 0); // 0 = Calendário (padrão)
    }

    public static void setWorkMode(Context context, int mode) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt("work_mode", mode); // 0 = Individual, 1 = Grupo
        editor.apply();
    }

    public static void setTourCompleted(Context context, boolean completed) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean("tour_completed", completed);
        editor.apply();
    }

    public static boolean isTourCompleted(Context context) {
        return getPreferences(context).getBoolean("tour_completed", false);
    }

    public static int getWorkMode(Context context) {
        return getPreferences(context).getInt("work_mode", 0);
    }

    public static void setActiveGroupId(Context context, String groupId) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString("active_group_id", groupId);
        editor.apply();
    }

    public static String getActiveGroupId(Context context) {
        return getPreferences(context).getString("active_group_id", "");
    }

    public static void setCachedRole(Context context, String groupId, String role) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString("cached_role_" + groupId, role);
        editor.putLong("cached_role_time_" + groupId, System.currentTimeMillis());
        editor.apply();
    }

    public static String getCachedRole(Context context, String groupId) {
        SharedPreferences sharedPre = getPreferences(context);
        long timestamp = sharedPre.getLong("cached_role_time_" + groupId, 0);
        long now = System.currentTimeMillis();
        // TTL de 5 minutos (5 * 60 * 1000 = 300000 ms)
        if (now - timestamp < 300000) {
            return sharedPre.getString("cached_role_" + groupId, null);
        }
        return null;
    }

    public static void clearCachedRole(Context context, String groupId) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.remove("cached_role_" + groupId);
        editor.remove("cached_role_time_" + groupId);
        editor.apply();
    }

    public static void setCategoryIcon(Context context, String categoryId, int resId) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt("category_icon_" + categoryId, resId);
        editor.apply();
    }

    public static int getCategoryIcon(Context context, String categoryId) {
        return getPreferences(context).getInt("category_icon_" + categoryId, 0);
    }
}
