package com.rogger.bipando.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rogger.bipando.data.model.Produto;

import java.util.List;

@Dao
public interface ProdutoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Produto produto);

    @Update
    void update(Produto produto);

    @Query("SELECT * FROM produtos WHERE deleted = 0 AND userId = :userId ORDER BY timestamp ASC")
    LiveData<List<Produto>> listarProdutosAtivos(String userId);

    @Query("SELECT * FROM produtos WHERE category = :categoria AND userId = :userId ORDER BY nome ASC")
    LiveData<List<Produto>> listarPorCategoria(String categoria, String userId);


    @Query("SELECT * FROM produtos WHERE deleted = 1 AND userId = :userId ORDER BY timestamp ASC")
    LiveData<List<Produto>> listarProdutosDeletados(String userId);

    @Query("UPDATE produtos SET deleted = 1, deletedAt = :deletedAt WHERE id = :id")
    void moverParaLixeira(int id, long deletedAt);
    // ♻️ Restaurar
    @Query("UPDATE produtos SET deleted = 0, deletedAt = NULL WHERE id = :id")
    void restaurarProduto(int id);

    @Query("DELETE FROM produtos WHERE id = :id")
    void removerPorId(int id);
}
