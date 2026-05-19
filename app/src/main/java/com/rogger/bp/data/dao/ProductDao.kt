package com.rogger.bp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM products ORDER BY timestamp DESC")
    fun getAllProducts(): Flow<List<PostProduct>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getProductsByCategory(categoryId: Int): Flow<List<PostProduct>>

    // Implementação da interface Cache<List<PostProduct>> para o DAO
    // Note: A interface Cache<T> foi adaptada para o contexto de um DAO que lida com uma lista de produtos.
    // Para 'get', 'isCached', 'remove', 'put' e 'clear', a 'key' pode ser usada para identificar um conjunto de dados (ex: 'all_products').
    // No entanto, para um DAO, é mais comum ter métodos específicos para operações de lista e item.
    // Para simplificar e manter a interface, vamos considerar que 'key' pode ser um identificador para a lista completa.
    // Para operações de item, os métodos específicos do DAO serão usados.

    // isCached para verificar se há algum produto no cache
    @Query("SELECT EXISTS(SELECT 1 FROM products LIMIT 1)")
    fun isAnyProductCached(): Boolean

    // get para retornar todos os produtos (representando o cache completo)
    @Query("SELECT * FROM products")
    fun getAllCachedProducts(): List<PostProduct>?

    // put para inserir/atualizar uma lista de produtos
    fun putAllProducts(products: List<PostProduct>) {
        insertAllProducts(products)
    }

    // remove para remover um produto específico (usando firestoreDocId como key)
    @Query("DELETE FROM products WHERE firestoreDocId = :key")
    fun removeCachedProduct(key: String)

    // clear para limpar todos os produtos
    @Query("DELETE FROM products")
    fun clearAllProducts()
}