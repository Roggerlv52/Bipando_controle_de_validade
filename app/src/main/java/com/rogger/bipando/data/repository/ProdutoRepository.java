package com.rogger.bipando.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.rogger.bipando.data.dao.ProdutoDao;
import com.rogger.bipando.data.database.BpdDatabase;
import com.rogger.bipando.data.model.Produto;

import java.util.List;

public class ProdutoRepository {

    private final ProdutoDao produtoDao;
    private final String userId;

    public ProdutoRepository(Application application) {
        BpdDatabase db = BpdDatabase.getDatabase(application);
        produtoDao = db.produtoDao();

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public LiveData<List<Produto>> getProdutosAtivos() {
        return produtoDao.listarProdutosAtivos(userId); // ← passa userId
    }

    public LiveData<List<Produto>> getProdutosDeletados() {
        return produtoDao.listarProdutosDeletados(userId); // ← passa userId
    }

    public void inserir(Produto produto) {
        produto.setUserId(userId); // ← NOVO: define o dono antes de salvar
        BpdDatabase.databaseWriteExecutor.execute(() ->
                produtoDao.insert(produto)
        );
    }

    public void atualizar(Produto produto) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            produtoDao.update(produto);
            Log.d("TAG", "Produto atualizado. Imagem: " + produto.getImagem());
        });
    }
    public void moverParaLixeira(int id) {
        BpdDatabase.databaseWriteExecutor.execute(() ->
                produtoDao.moverParaLixeira(
                        id,
                        System.currentTimeMillis()
                )
        );
    }
    public void restaurar(int id) {
        BpdDatabase.databaseWriteExecutor.execute(() ->
                produtoDao.restaurarProduto(id)
        );
    }

    public void excluirDefinitivoPorId(int id) {
        BpdDatabase.databaseWriteExecutor.execute(() ->
                produtoDao.removerPorId(id)
        );
    }
}

