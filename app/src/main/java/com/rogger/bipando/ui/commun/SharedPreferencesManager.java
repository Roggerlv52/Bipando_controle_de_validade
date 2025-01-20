package com.rogger.bipando.ui.commun;

import static androidx.browser.trusted.sharing.ShareTarget.FileFormField.KEY_NAME;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class SharedPreferencesManager {
    private static final String PREF_NAME = "shared_key_date";
    // Método para adicionar um valor ao SharedPreferences
    public static void sharedBeepState(Context context, String key, boolean beep) {
        SharedPreferences sharedp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedp.edit();
        editor.putBoolean(key,beep);
        editor.apply();
    }
    // Método para atualizar um  valor do SharedPreferences
    public static void updateThemeNumber(Context context, String key, int newThemeNumber) {
        SharedPreferences sharedPre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPre.edit();
        editor.putInt(key,newThemeNumber);
        editor.apply();
    }

    // Método para recuperar um valor do SharedPreferences
    public static int getThemeNumber(Context context, String key) {
        SharedPreferences sharedPre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPre.getInt(key, 0);
    }
    public static boolean getBeepState(Context context, String key) {
        SharedPreferences sharedPre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPre.getBoolean(key, false);
    }
    public static void setloginState(Context context,String key,boolean state){
        SharedPreferences sharedp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedp.edit();
        editor.putBoolean(key,state);
        editor.apply();

    }
    public static boolean getloginState(Context context,String key){
        SharedPreferences sharedPre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPre.getBoolean(key, false);
    }
    public static void saveUserInfo(Context context,String name,String imageUrl){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nameUser",name);
        editor.putString("profile_image_url", imageUrl);
        editor.apply();
    }
    public static List<String> getUserInfo(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<String> userInfo = new ArrayList<>();
        userInfo.add(sharedPreferences.getString("nameUser", "Nome não encontrado"));
        userInfo.add(sharedPreferences.getString("profile_image_url", null));
        return userInfo;
    }
}
