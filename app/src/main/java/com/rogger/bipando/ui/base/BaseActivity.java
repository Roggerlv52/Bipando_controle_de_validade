package com.rogger.bipando.ui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rogger.bipando.R;
import com.rogger.bipando.ui.commun.SharedPreferencesManager;

public abstract class BaseActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle arg0) {
		int themeNumber = SharedPreferencesManager.getThemeNumber(this,"chave");
		switch (themeNumber){
			case 1:
				setTheme(R.style.Theme_Bipando);
				break;
			case 2:
				setTheme(R.style.Theme_Bipando_2);
				break;
			case 3:
				setTheme(R.style.Theme_Bipando_3);
				break;
			case 4:
				setTheme(R.style.Theme_Bipando_4);
				break;
		}
		super.onCreate(arg0);
	}
	protected void showCustomToast() {
		LayoutInflater inflater = getLayoutInflater();
		View view = inflater.inflate(R.layout.custom_toast,findViewById(R.id.toast_image));
		Toast toast = new Toast(getApplicationContext());
		toast.setDuration(Toast.LENGTH_SHORT);
		toast.setView(view);
		toast.setGravity(android.view.Gravity.CENTER, 0, 0);
		toast.show();
	}

}