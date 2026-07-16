package com.rogger.bp.ui.base;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Window;

import androidx.annotation.AttrRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.rogger.bp.R;
import com.rogger.bp.ui.commun.SharedPreferencesManager;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle arg0) {
        int themeNumber = SharedPreferencesManager.getThemeNumber(this, "chave");
        switch (themeNumber) {
            case 1:
                setTheme(R.style.Theme_Bpd);
                break;
            case 2:
                setTheme(R.style.Theme_Bpd_2);
                break;
            case 3:
                setTheme(R.style.Theme_Bpd_3);
                break;
            case 4:
                setTheme(R.style.Theme_Bpd_4);
                break;
        }
        super.onCreate(arg0);
    }

    protected void enableEdgeToEdge() {
        Window window = this.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // ✅ CORREÇÃO SÉNIOR: Ignora chamadas descontinuadas no Android 15+ (API 35+)
        if (android.os.Build.VERSION.SDK_INT < 35) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    protected void restoreSystemBars() {
        Window window = this.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);

        // ✅ CORREÇÃO SÉNIOR: Ignora chamadas descontinuadas no Android 15+ (API 35+)
        if (android.os.Build.VERSION.SDK_INT < 35) {
            window.setStatusBarColor(getThemeColor(R.attr._color_theme_status));
            window.setNavigationBarColor(getThemeColor(R.attr._color_theme_navigation));
        }

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());

        if (controller != null) {
            controller.show(WindowInsetsCompat.Type.systemBars());
        }
    }

    private int getThemeColor(@AttrRes int attrColor) {
        TypedValue typedValue = new TypedValue();
        this
                .getTheme()
                .resolveAttribute(attrColor, typedValue, true);
        return typedValue.data;
    }
}