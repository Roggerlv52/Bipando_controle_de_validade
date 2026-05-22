package com.rogger.bp.ui.category.data

import com.rogger.bp.data.dao.Cache
import com.rogger.bp.data.dao.CategoryDao
import com.rogger.bp.data.model.PostCategory
import kotlinx.coroutines.flow.Flow

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 19/05/2026
 * Hora: 09:58
 */
class RoomCategoryCache(private val categoryDao: CategoryDao) : Cache<List<PostCategory>> {

    override fun isCached(key: String): Boolean {
        return categoryDao.isAnyCategoryCached()
    }

    override fun get(key: String): List<PostCategory>? {
        return categoryDao.getAllCachedCategories()
    }

    override fun put(key: String, data: List<PostCategory>) {
        categoryDao.putAllCategoriesSync(data)
    }

    // --- MÉTODOS ADICIONAIS PARA O REPOSITÓRIO ---

    fun getAllCategoriesFlow(): Flow<List<PostCategory>> {
        return categoryDao.getAllCategories()
    }

    suspend fun putAllCategories(categories: List<PostCategory>) {
        categoryDao.putAllCategories(categories)
    }

    suspend fun replaceAllCategories(categories: List<PostCategory>) {
        categoryDao.replaceAllCategories(categories)
    }

    suspend fun insertCategory(category: PostCategory) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: PostCategory) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: PostCategory) {
        categoryDao.deleteCategory(category)
    }

    override suspend fun remove(key: String) {
        if (key.isNotEmpty()) {
            categoryDao.removeCachedCategory(key)
        } else {
            categoryDao.clearAllCategories()
        }
    }

    override suspend fun clear() {
        categoryDao.clearAllCategories()
    }
}
