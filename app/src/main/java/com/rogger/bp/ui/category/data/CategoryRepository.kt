package com.rogger.bp.ui.category.data

import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.dao.ProductDao
import com.rogger.bp.data.model.PostCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class CategoryRepository(
    private val remoteDataSource: PostCategoryDataSource,
    private val localCache: RoomCategoryCache,
    private val productDao: ProductDao
) {

    private var categoryListenerRegistration: ListenerRegistration? = null
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun fetchAll(callback: FetchCategoriesCallback) {

        stopListeningForCategories()

        repositoryScope.launch {
            localCache.getAllCategoriesFlow().firstOrNull()?.let { cached ->
                if (cached.isNotEmpty()) callback.onSuccess(cached)
            }
            // Agora stopListeningForCategories() dentro deste escopo é seguro
            categoryListenerRegistration =
                remoteDataSource.addCategoriesSnapshotListener(object : FetchCategoriesCallback {
                    override fun onSuccess(categories: List<PostCategory>) {
                        CoroutineScope(Dispatchers.IO).launch {

                            localCache.replaceAllCategories(categories)
                        }
                    }

                    override fun onFailure(message: String) {
                    }

                    override fun onComplete() {
                    }

                })
        }
    }

    fun create(category: PostCategory, callback: CategoryCallback) {
        // 1. Salva no cache local do Room imediatamente
        CoroutineScope(Dispatchers.IO).launch {
            localCache.insertCategory(category)
        }

        // 2. Envia para o Firestore em segundo plano
        remoteDataSource.createCategory(category, callback)

    }

    fun update(category: PostCategory, callback: CategoryCallback) {
        // 1. Atualiza no cache local do Room imediatamente
        CoroutineScope(Dispatchers.IO).launch {
            localCache.updateCategory(category)
        }

        // 2. Envia para o Firestore em segundo plano
        remoteDataSource.updateCategory(category, callback)
    }

    fun delete(category: PostCategory, callback: CategoryCallback) {
        remoteDataSource.deleteCategory(category, object : CategoryCallback {
            override fun onSuccess(p: PostCategory) {
                // Não deletar manualmente do cache aqui.
                // O snapshot listener já atualiza o Room via replaceAllCategories.
                callback.onSuccess(p)
            }

            override fun onFailure(message: String) = callback.onFailure(message)
            override fun onComplete() = callback.onComplete()
            override fun onAlreadyExists(existingCategory: PostCategory) =
                callback.onAlreadyExists(existingCategory)
        })
    }

    fun stopListeningForCategories() {
        categoryListenerRegistration?.remove()
        categoryListenerRegistration = null
    }

    fun getCachedCategoriesFlow(): Flow<List<PostCategory>> {
        return localCache.getAllCategoriesFlow()
    }

    fun getCachedCategoriesWithCountsFlow(): Flow<List<PostCategory>> {
        return localCache.getAllCategoriesFlow().combine(productDao.getAllProducts()) { categories, products ->
            // Agrupa os produtos ativos por id de categoria e gera um mapa de contagem rápido
            val countsMap = products.groupingBy { it.categoryId }.eachCount()

            // Popula o campo itemCount de cada categoria
            categories.map { category ->
                category.itemCount = countsMap[category.firestoreId] ?: 0
                category
            }
        }
    }

    fun destroy() {
        stopListeningForCategories()
        repositoryScope.cancel()
    }
}