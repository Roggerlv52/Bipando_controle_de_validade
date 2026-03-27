package com.rogger.bp.ui.category;

import android.app.AlertDialog;
import android.content.Context;

public class DialogUtil {

    public interface OnConfirmListener {
        void onConfirm();
    }

    public static void showConfirmDialog(
            Context context,
            String mensagem,
            OnConfirmListener listener
    ) {
        new AlertDialog.Builder(context)
                .setMessage(mensagem)
                .setCancelable(false)
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton("Aceitar", (dialog, which) -> {
                    if (listener != null) {
                        listener.onConfirm();
                    }
                    dialog.dismiss();
                })
                .show();
    }
}

