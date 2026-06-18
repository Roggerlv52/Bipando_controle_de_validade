package com.rogger.bp.ui.base;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bp.R;
import com.rogger.bp.data.model.PostCategory;

import java.util.List;

public class CategoriaDialogUtil {

    public interface CategoriaCallback {
        void onCategoriaSelecionada(String categoriaId);
        void onAdicionarCategoria();
    }

    public static void mostrarDialogo(
            Context context,
            List<PostCategory> categorias,
            CategoriaCallback callback
    ) {

        // 👉 Se não houver categorias
        if (categorias == null || categorias.isEmpty()) {
            if (callback != null) callback.onAdicionarCategoria();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.dialog_title_select));

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        // 👉 cria o dialog primeiro
        AlertDialog dialog = builder.create();

        CategoriaAdapter adapter = new CategoriaAdapter(categorias, categoria -> {

            // 🔥 FECHA O DIALOG
            dialog.dismiss();

            if (callback != null) {
                callback.onCategoriaSelecionada(categoria.getFirestoreId());
            }
        });

        recyclerView.setAdapter(adapter);

        dialog.setView(recyclerView);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getText(R.string.dialog_button_cancel),
                (d, which) -> d.dismiss());

        dialog.show();
    }
}