package com.rogger.bipando.ui.base;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bipando.data.model.Categoria;

import java.util.List;

public class CategoriaDialogUtil {

    public interface CategoriaCallback {
        void onCategoriaSelecionada(int categoriaId);
        void onAdicionarCategoria();
    }

    public static void mostrarDialogo(
            Context context,
            List<Categoria> categorias,
            CategoriaCallback callback
    ) {

        // 👉 Se não houver categorias
        if (categorias == null || categorias.isEmpty()) {
            if (callback != null) callback.onAdicionarCategoria();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Selecione uma Categoria");

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        // 👉 cria o dialog primeiro
        AlertDialog dialog = builder.create();

        CategoriaAdapter adapter = new CategoriaAdapter(categorias, categoria -> {

            // 🔥 FECHA O DIALOG
            dialog.dismiss();

            if (callback != null) {
                callback.onCategoriaSelecionada(categoria.getId());
            }
        });

        recyclerView.setAdapter(adapter);

        dialog.setView(recyclerView);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancelar",
                (d, which) -> d.dismiss());

        dialog.show();
    }
}