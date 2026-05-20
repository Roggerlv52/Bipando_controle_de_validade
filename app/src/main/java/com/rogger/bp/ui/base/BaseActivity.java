package com.rogger.bp.ui.base;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.rogger.bp.R;
import com.rogger.bp.ui.commun.SharedPreferencesManager;

public abstract class BaseActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle arg0) {
		int themeNumber = SharedPreferencesManager.getThemeNumber(this,"chave");
		switch (themeNumber){
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
}