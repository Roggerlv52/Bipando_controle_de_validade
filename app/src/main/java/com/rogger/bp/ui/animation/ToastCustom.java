package com.rogger.bp.ui.animation;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;


import com.rogger.bp.R;

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 20/05/2026
 * Hora: 11:45
 */
public class ToastCustom {
    public static void showCustomToast(Context context, String mensagem) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.custom_toast, null);
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}