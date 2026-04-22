package com.rogger.bp.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rogger.bp.data.model.Produto;
import com.rogger.bp.data.model.ProdutoWithCategory;

import java.util.List;

@Dao
public interface ProdutoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Produto produto);

    @Update
    void update(Produto produto);

    @Query("SELECT p.*, c.nome AS nomeCategoria " +
            "FROM produtos p " +
            "LEFT JOIN categorias c ON p.categoryId = c.id " +
            "WHERE p.deleted = 0 AND p.userId = :userId " +
            "AND p.nome LIKE '%' || :query || '%' " +
            "ORDER BY p.nome ASC")
    LiveData<List<ProdutoWithCategory>> buscarPorNome(String userId, String query);

    // 🔍 Busca por código de barras (suporta busca parcial via LIKE)
    @Query("SELECT p.*, c.nome AS nomeCategoria " +
            "FROM produtos p " +
            "LEFT JOIN categorias c ON p.categoryId = c.id " +
            "WHERE p.deleted = 0 AND p.userId = :userId " +
            "AND p.codigoBarras LIKE '%' || :barcode || '%' " +
            "ORDER BY p.nome ASC")
    LiveData<List<ProdutoWithCategory>> buscarPorCodigoBarras(String userId, String barcode);

    // 🔑 JOIN com categorias para buscar o nomeCategoria atualizado
    @Query("SELECT p.*, c.nome AS nomeCategoria " +
            "FROM produtos p " +
            "LEFT JOIN categorias c ON p.categoryId = c.id " +
            "WHERE p.deleted = 0 AND p.userId = :userId " +
            "ORDER BY p.timestamp ASC")
    LiveData<List<ProdutoWithCategory>> listarProdutosAtivos(String userId);

    // ✅ Versão síncrona para o ExpirationWorker
    @Query("SELECT p.*, c.nome AS nomeCategoria " +
            "FROM produtos p " +
            "LEFT JOIN categorias c ON p.categoryId = c.id " +
            "WHERE p.deleted = 0 AND p.userId = :userId " +
            "ORDER BY p.timestamp ASC")
    List<Produto> listarProdutosAtivosSync(String userId);

    // ────────────────────────────────────────────────────────────────
    // 🆕 BUSCA POR NOME DE CATEGORIA
    // Retorna todos os produtos cujo nome da categoria contenha :query
    // (case-insensitive via LIKE, sem acentos comparados literalmente)
    // ────────────────────────────────────────────────────────────────
    @Query("SELECT p.*, c.nome AS nomeCategoria " +
            "FROM produtos p " +
            "LEFT JOIN categorias c ON p.categoryId = c.id " +
            "WHERE p.deleted = 0 AND p.userId = :userId " +
            "AND LOWER(c.nome) LIKE LOWER('%' || :query || '%') " +
            "ORDER BY p.nome ASC")
    LiveData<List<ProdutoWithCategory>> buscarPorNomeCategoria(String userId, String query);


    @Query("SELECT p.*, c.nome AS nomeCategoria " +
            "FROM produtos p " +
            "LEFT JOIN categorias c ON p.categoryId = c.id " +
            "WHERE p.deleted = 1 AND p.userId = :userId " +
            "ORDER BY p.timestamp ASC")
    LiveData<List<ProdutoWithCategory>> listarProdutosDeletados(String userId);

    @Query("SELECT p.*, c.nome AS nomeCategoria " +
            "FROM produtos p " +
            "LEFT JOIN categorias c ON p.categoryId = c.id " +
            "WHERE p.id = :id LIMIT 1")
    ProdutoWithCategory buscarPorIdSync(int id);

    @Query("UPDATE produtos SET deleted = 1, deletedAt = :deletedAt WHERE id = :id")
    void moverParaLixeira(int id, long deletedAt);

    // ♻️ Restaurar
    @Query("UPDATE produtos SET deleted = 0, deletedAt = NULL WHERE id = :id")
    void restaurarProduto(int id);

    @Query("DELETE FROM produtos WHERE id = :id")
    void removerPorId(int id);

    @Query("SELECT COUNT(*) FROM produtos WHERE categoryId = :categoryId AND deleted = 0 AND userId = :userId")
    int contarProdutosPorCategoria(int categoryId, String userId);

    @Query("SELECT COUNT(*) FROM produtos WHERE deleted = 0 AND userId = :userId")
    LiveData<Integer> getCountAtivos(String userId);
}
