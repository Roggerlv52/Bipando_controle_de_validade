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
        // Como o método da interface não é suspend, usamos runBlocking ou
        // idealmente chamamos o método suspend diretamente no repositório.
        // Para fins de compatibilidade com a interface:
        categoryDao.putAllCategoriesSync(data)
    }

    // --- MÉTODOS ADICIONAIS PARA O REPOSITÓRIO ---

    /**
     * Retorna um Flow para observação reativa dos dados no banco local.
     * É este método que resolve o erro "em vermelho" no seu CategoryRepository.
     */
    fun getAllCategoriesFlow(): Flow<List<PostCategory>> {
        return categoryDao.getAllCategories()
    }

    /**
     * Versão suspend para ser usada dentro de Coroutines no Repositório.
     */
    suspend fun putAllCategories(categories: List<PostCategory>) {
        categoryDao.putAllCategories(categories)
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

    // --- FIM DOS MÉTODOS ADICIONAIS ---

    override fun remove(key: String) {
        if (key.isNotEmpty()) {
            categoryDao.removeCachedCategory(key)
        } else {
            categoryDao.clearAllCategories()
        }
    }

    override fun clear() {
        categoryDao.clearAllCategories()
    }
}