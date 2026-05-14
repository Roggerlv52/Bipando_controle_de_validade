package com.rogger.bp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 20:20
 */
interface PostProductDao {
    @Dao
    interface PostProductDao {

        // ── Escrita ───────────────────────────────────────────────────────────

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(product: PostProduct): Long

        @Update
        fun update(product: PostProduct)

        // ── Leitura activos ───────────────────────────────────────────────────

        @Query("""
        SELECT p.*, c.nome AS categoryName
        FROM produtos p
        LEFT JOIN categorias c ON p.categoryId = c.id
        WHERE p.deleted = 0 AND p.userId = :userId
        ORDER BY p.timestamp ASC
    """)
        fun listActiveProducts(userId: String): LiveData<List<PostProduct>>

        @Query("""
        SELECT p.*, c.nome AS categoryName
        FROM produtos p
        LEFT JOIN categorias c ON p.categoryId = c.id
        WHERE p.deleted = 0 AND p.userId = :userId
        ORDER BY p.timestamp ASC
    """)
        fun listActiveProductsSync(userId: String): List<PostProduct>

        // ── Leitura deletados ─────────────────────────────────────────────────

        @Query("""
        SELECT p.*, c.nome AS categoryName
        FROM produtos p
        LEFT JOIN categorias c ON p.categoryId = c.id
        WHERE p.deleted = 1 AND p.userId = :userId
        ORDER BY p.timestamp ASC
    """)
        fun listDeletedProducts(userId: String): LiveData<List<PostProduct>>

        // ── Busca ─────────────────────────────────────────────────────────────

        @Query("""
        SELECT p.*, c.nome AS categoryName
        FROM produtos p
        LEFT JOIN categorias c ON p.categoryId = c.id
        WHERE p.deleted = 0 AND p.userId = :userId
        AND p.nome LIKE '%' || :query || '%'
        ORDER BY p.nome ASC
    """)
        fun searchByName(userId: String, query: String): LiveData<List<PostProduct>>

        @Query("""
        SELECT p.*, c.nome AS categoryName
        FROM produtos p
        LEFT JOIN categorias c ON p.categoryId = c.id
        WHERE p.deleted = 0 AND p.userId = :userId
        AND p.codigoBarras LIKE '%' || :barcode || '%'
        ORDER BY p.nome ASC
    """)
        fun searchByBarcode(userId: String, barcode: String): LiveData<List<PostProduct>>

        @Query("""
        SELECT p.*, c.nome AS categoryName
        FROM produtos p
        LEFT JOIN categorias c ON p.categoryId = c.id
        WHERE p.deleted = 0 AND p.userId = :userId
        AND LOWER(c.nome) LIKE LOWER('%' || :query || '%')
        ORDER BY p.nome ASC
    """)
        fun searchByCategoryName(userId: String, query: String): LiveData<List<PostProduct>>

        // ── Busca por id ──────────────────────────────────────────────────────

        @Query("""
        SELECT p.*, c.nome AS categoryName
        FROM produtos p
        LEFT JOIN categorias c ON p.categoryId = c.id
        WHERE p.id = :id
        LIMIT 1
    """)
        fun findById(id: Int): LiveData<PostProduct?>

        @Query("""
        SELECT p.*, c.nome AS categoryName
        FROM produtos p
        LEFT JOIN categorias c ON p.categoryId = c.id
        WHERE p.id = :id
        LIMIT 1
    """)
        fun findByIdSync(id: Int): PostProduct?

        // ── Soft-delete / Restauro ────────────────────────────────────────────

        @Query("UPDATE produtos SET deleted = 1, deletedAt = :deletedAt WHERE id = :id")
        fun moveToTrash(id: Int, deletedAt: Long)

        @Query("UPDATE produtos SET deleted = 0, deletedAt = NULL WHERE id = :id")
        fun restore(id: Int)

        @Query("DELETE FROM produtos WHERE id = :id")
        fun deleteById(id: Int)

        // ── Contagens ─────────────────────────────────────────────────────────

        @Query("SELECT COUNT(*) FROM produtos WHERE deleted = 0 AND userId = :userId")
        fun countActive(userId: String): LiveData<Int>

        @Query("SELECT COUNT(*) FROM produtos WHERE deleted = 1 AND userId = :userId")
        fun countDeleted(userId: String): LiveData<Int>
    }
}