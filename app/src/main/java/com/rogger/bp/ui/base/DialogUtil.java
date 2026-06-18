package com.rogger.bp.ui.base;


import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.rogger.bp.R;
import com.rogger.bp.data.model.PostCategory;

import java.util.List;

public class DialogUtil {

    public interface OnConfirmListener {
        void onConfirm();
    }

    public interface OnCategoriaSelected {
        void onSelected(PostCategory categoria);
    }

    public static void showDeleteDialog(
            @NonNull Context context,
            @NonNull String mensagem,
            @NonNull OnConfirmListener listener
    ) {

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.dialog_title))
                .setMessage(mensagem)
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.excluir), (dialog, which) -> {
                    dialog.dismiss();
                    listener.onConfirm();
                })
                .setNegativeButton(context.getString(R.string.dialog_bottom_cancel), (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void showCustomDialog(@NonNull Context context,
                                        @NonNull String message,
                                        @NonNull OnConfirmListener listener) {

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        TextView textView = new TextView(context);
        textView.setText(message);
        textView.setTextSize(16);
        textView.setTextColor(Color.RED);
        textView.setPadding(0, 0, 0, 30);

        layout.addView(textView);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(layout)
                .setCancelable(true)
                .setPositiveButton(context.getString(R.string.dialog_bottom_accept), (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                    listener.onConfirm();
                })
                .setNegativeButton(context.getString(R.string.dialog_bottom_cancel), (d, which) -> d.dismiss())
                .create();

        dialog.show();
    }

    public static void show(
            Context context,
            List<PostCategory> categorias,
            OnCategoriaSelected callback
    ) {
        String[] nomes = new String[categorias.size()];
        for (int i = 0; i < categorias.size(); i++) {
            nomes[i] = categorias.get(i).getName();
        }

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.dialog_title_select))
                .setItems(nomes, (dialog, which) -> callback.onSelected(categorias.get(which)))
                .setNegativeButton(context.getString(R.string.dialog_bottom_cancel), null)
                .show();
    }
}

