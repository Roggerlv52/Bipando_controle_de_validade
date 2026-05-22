package com.rogger.bp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rogger.bp.data.model.PostCategory
import kotlinx.coroutines.flow.Flow

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 18/05/2026
 * Hora: 14:17
 */
@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE firestoreId = :key")
    fun getCategoryByDocId(key: String): PostCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: PostCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllCategories(categories: List<PostCategory>)

    @Update
    suspend fun updateCategory(category: PostCategory)

    @Delete
    suspend fun deleteCategory(category: PostCategory)

    @Query("DELETE FROM categories WHERE firestoreId = :key")
    suspend fun removeCategory(key: String)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<PostCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAllCategories(categories: List<PostCategory>)

    // Método não-suspend para a interface Cache
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putAllCategoriesSync(categories: List<PostCategory>)

    @Query("SELECT EXISTS(SELECT 1 FROM categories LIMIT 1)")
    fun isAnyCategoryCached(): Boolean

    @Query("SELECT * FROM categories")
    fun getAllCachedCategories(): List<PostCategory>?

    @Query("DELETE FROM categories WHERE firestoreId = :key")
    fun removeCachedCategory(key: String)

    @Query("DELETE FROM categories")
    fun clearAllCategories()

    /**
     * Substitui TODA a tabela de forma atômica (clear + insert em uma transação).
     *
     * Isso é fundamental para evitar duplicação no AdapterCategory:
     * - INSERT OR REPLACE (putAllCategories) pode emitir o Flow múltiplas vezes,
     *   uma por linha substituída, causando flashes e itens duplicados na UI.
     * - @Transaction garante que o Room só notifica o Flow UMA vez, ao final,
     *   com o estado final completo e correto.
     */
    @Transaction
    suspend fun replaceAllCategories(categories: List<PostCategory>) {
        clearAllCategories()
        putAllCategories(categories)
    }
}
