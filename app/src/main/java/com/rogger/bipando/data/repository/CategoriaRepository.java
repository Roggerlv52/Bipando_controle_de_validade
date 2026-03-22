package com.rogger.bipando.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.rogger.bipando.data.dao.CategoriaDao;
import com.rogger.bipando.data.database.BpdDatabase;
import com.rogger.bipando.data.model.Categoria;

import java.util.List;

public class CategoriaRepository {

    private CategoriaDao categoriaDao;
    private final String userId; // 🔑 uid do usuário logado

    public CategoriaRepository(Application app) {
        BpdDatabase db = BpdDatabase.getDatabase(app);
        categoriaDao = db.categoriaDao();

        // 🔑 Captura o uid do Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public LiveData<List<Categoria>> getCategorias() {
        return categoriaDao.listarCategorias(userId); // 🔑
    }

    public void inserir(Categoria categoria) {
        categoria.setUserId(userId); // 🔑 define o dono antes de salvar
        BpdDatabase.databaseWriteExecutor.execute(() ->
                categoriaDao.inserirCategoria(categoria)
        );
    }

    public void atualizar(Categoria categoria) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            categoriaDao.atualizarCategoria(categoria);
            Log.d("CategoriaRepository", "Atualizando categoria: " + categoria.getNome());
        });
    }

    public void remover(Categoria categoria) {
        BpdDatabase.databaseWriteExecutor.execute(() ->
                categoriaDao.removerCategoria(categoria)
        );
    }
}

