package com.rogger.bp.ui.base;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.rogger.bp.data.model.Categoria;

import java.util.List;

public class DialogUtil {
    // Interface de callback
    public interface OnConfirmListener {
        void onConfirm();
    }

    public interface OnCategoriaSelected {
        void onSelected(Categoria categoria);
    }

    /**
     * Dialog padrão de confirmação para deletar item
     */
    public static void showDeleteDialog(
            @NonNull Context context,
            @NonNull String mensagem,
            @NonNull OnConfirmListener listener
    ) {

        new AlertDialog.Builder(context)
                .setTitle("Confirmar exclusão")
                .setMessage(mensagem)
                .setCancelable(false)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    dialog.dismiss();
                    listener.onConfirm();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void show(
            Context context,
            List<Categoria> categorias,
            OnCategoriaSelected callback
    ) {
        String[] nomes = new String[categorias.size()];
        for (int i = 0; i < categorias.size(); i++) {
            nomes[i] = categorias.get(i).getNome();
        }

        new AlertDialog.Builder(context)
                .setTitle("Selecione uma categoria")
                .setItems(nomes, (dialog, which) -> {
                    callback.onSelected(categorias.get(which));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}

