package com.rogger.bp.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rogger.bp.data.model.Produto;

import java.util.List;

@Dao
public interface ProdutoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Produto produto);

    @Update
    void update(Produto produto);

    // 🔑 JOIN com categorias para buscar o nomeCategoria atualizado
    @Query("SELECT p.*, c.nome AS nomeCategoria " +
            "FROM produtos p " +
            "LEFT JOIN categorias c ON p.categoryId = c.id " +
            "WHERE p.deleted = 0 AND p.userId = :userId " +
            "ORDER BY p.timestamp ASC")
    LiveData<List<Produto>> listarProdutosAtivos(String userId);

    // ✅ Versão síncrona para o ExpirationWorker
    @Query("SELECT p.*, c.nome AS nomeCategoria " +
            "FROM produtos p " +
            "LEFT JOIN categorias c ON p.categoryId = c.id " +
            "WHERE p.deleted = 0 AND p.userId = :userId " +
            "ORDER BY p.timestamp ASC")
    List<Produto> listarProdutosAtivosSync(String userId);

    @Query("SELECT p.*, c.nome AS nomeCategoria " +
            "FROM produtos p " +
            "LEFT JOIN categorias c ON p.categoryId = c.id " +
            "WHERE p.categoryId = :categoria AND p.userId = :userId " +
            "ORDER BY p.nome ASC")
    LiveData<List<Produto>> listarPorCategoria(String categoria, String userId);


    @Query("SELECT p.*, c.nome AS nomeCategoria " +
            "FROM produtos p " +
            "LEFT JOIN categorias c ON p.categoryId = c.id " +
            "WHERE p.deleted = 1 AND p.userId = :userId " +
            "ORDER BY p.timestamp ASC")
    LiveData<List<Produto>> listarProdutosDeletados(String userId);

    @Query("UPDATE produtos SET deleted = 1, deletedAt = :deletedAt WHERE id = :id")
    void moverParaLixeira(int id, long deletedAt);
    // ♻️ Restaurar
    @Query("UPDATE produtos SET deleted = 0, deletedAt = NULL WHERE id = :id")
    void restaurarProduto(int id);

    @Query("DELETE FROM produtos WHERE id = :id")
    void removerPorId(int id);
}
