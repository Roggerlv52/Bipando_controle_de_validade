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

    @Query("SELECT * FROM products WHERE firestoreDocId = :uuid")
    fun getProductByUuidFlow(uuid: String): Flow<PostProduct?>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): PostProduct?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: PostProduct)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllProducts(products: List<PostProduct>)

    @Update
    suspend fun updateProduct(product: PostProduct)

    @Delete
    suspend fun deleteProduct(product: PostProduct)

    // ✅ ATUALIZAÇÃO: Consulta reativa de TODOS os produtos (ativos + lixeira) para controle do limite Premium
    @Query("SELECT COUNT(*) FROM products WHERE groupId = :groupId")
    fun getTotalProductsCountLiveData(groupId: String): androidx.lifecycle.LiveData<Int>

    @Query("SELECT COUNT(*) FROM products")
    fun getGlobalTotalProductsCountLiveData(): androidx.lifecycle.LiveData<Int>

    @Query("DELETE FROM products WHERE firestoreDocId = :key")
    suspend fun removeProduct(key: String)

    @Query("UPDATE products SET categoryName = :newName WHERE categoryId = :categoryId AND groupId = :groupId")
    suspend fun updateCategoryNameInProducts(categoryId: String, newName: String, groupId: String)

    @Query("UPDATE products SET categoryName = '', categoryId = '' WHERE categoryId = :categoryId AND groupId = :groupId")
    suspend fun clearCategoryInProducts(categoryId: String, groupId: String)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM products WHERE groupId = :groupId")
    suspend fun clearProductsByGroup(groupId: String)

    @Query("UPDATE products SET groupId = :newGroupId WHERE groupId = '' OR groupId IS NULL")
    suspend fun updateIndividualProductsGroupId(newGroupId: String)

    // 👉 Consulta reativa de produtos ativos (Home)
    @Query("SELECT COUNT(*) FROM products WHERE deleted = 0 AND groupId = :groupId")
    fun getActiveProductsCountLiveData(groupId: String): androidx.lifecycle.LiveData<Int>

    // 👉 Consulta reativa de contagem de produtos deletados ou não
    @Query("SELECT COUNT(*) FROM products WHERE deleted = :isDeleted AND groupId = :groupId")
    fun getDeletedProductsCountLiveData(isDeleted: Boolean, groupId: String): androidx.lifecycle.LiveData<Int>

    // 👉 Consulta reativa de produtos na lixeira
    @Query("SELECT * FROM products WHERE deleted = 1 AND groupId = :groupId ORDER BY timestamp DESC")
    fun getDeletedProductsFlow(groupId: String): Flow<List<PostProduct>>

    // Apenas produtos não deletados
    @Query("SELECT * FROM products WHERE deleted = 0 AND groupId = :groupId ORDER BY timestamp DESC")
    fun getAllProducts(groupId: String): Flow<List<PostProduct>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND deleted = 0 AND groupId = :groupId ORDER BY timestamp DESC")
    fun getProductsByCategory(categoryId: String, groupId: String): Flow<List<PostProduct>>

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
    suspend fun clearAllProducts()
    @Query("SELECT * FROM products WHERE deleted = 0 AND groupId = :groupId AND name LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchProductsByName(query: String, groupId: String): Flow<List<PostProduct>>

    @Query("SELECT * FROM products WHERE deleted = 0 AND groupId = :groupId AND barcode LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchProductsByBarcode(query: String, groupId: String): Flow<List<PostProduct>>

    @Query("SELECT * FROM products WHERE deleted = 0 AND groupId = :groupId AND categoryName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchProductsByCategoryName(query: String, groupId: String): Flow<List<PostProduct>>

    @Query("SELECT * FROM products WHERE deleted = 0 AND groupId = :groupId AND (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR categoryName LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchAllFields(query: String, groupId: String): Flow<List<PostProduct>>

    @Transaction
    suspend fun replaceAllProducts(products: List<PostProduct>) {
        clearAllProducts()
        insertAllProducts(products)
    }

    @Transaction
    suspend fun replaceAllProductsByGroup(groupId: String, products: List<PostProduct>) {
        clearProductsByGroup(groupId)
        insertAllProducts(products)
    }
}