package com.rogger.bp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rogger.bp.data.model.PostProduct
import kotlinx.coroutines.flow.Flow

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 18/05/2026
 * Hora: 16:00
 */
@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE firestoreDocId = :key")
    fun getProductByDocId(key: String): PostProduct?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: PostProduct)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllProducts(products: List<PostProduct>)

    @Update
    suspend fun updateProduct(product: PostProduct)

    @Delete
    suspend fun deleteProduct(product: PostProduct)

    @Query("DELETE FROM products WHERE firestoreDocId = :key")
    suspend fun removeProduct(key: String)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    // Apenas produtos não deletados — garante que o Flow nunca exponha
    // itens com deleted=true para a HomeFragment.
    @Query("SELECT * FROM products WHERE deleted = 0 ORDER BY timestamp DESC")
    fun getAllProducts(): Flow<List<PostProduct>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND deleted = 0 ORDER BY timestamp DESC")
    fun getProductsByCategory(categoryId: Int): Flow<List<PostProduct>>

    // ── Métodos do Cache ──────────────────────────────────────────────────

    @Query("SELECT EXISTS(SELECT 1 FROM products LIMIT 1)")
    fun isAnyProductCached(): Boolean

    @Query("SELECT * FROM products")
    fun getAllCachedProducts(): List<PostProduct>?

    fun putAllProducts(products: List<PostProduct>) {
        insertAllProducts(products)
    }

    @Query("DELETE FROM products WHERE firestoreDocId = :key")
    fun removeCachedProduct(key: String)

    @Query("DELETE FROM products")
    fun clearAllProducts()

    /**
     * BUGFIX: Apaga e reinsere em uma única transação Room.
     *
     * A anotação @Transaction garante que o Flow de getAllProducts()
     * não emita uma lista vazia entre o clear e o insert — Room
     * só notifica os observers depois que a transação completa.
     */
    @Transaction
    fun replaceAllProducts(products: List<PostProduct>) {
        clearAllProducts()
        insertAllProducts(products)
    }
}