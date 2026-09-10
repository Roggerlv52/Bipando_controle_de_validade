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
import kotlinx.coroutines.withContext

class CategoryRepository(
    private val remoteDataSource: PostCategoryDataSource,
    private val localCache: RoomCategoryCache,
    private val productDao: ProductDao,
    private val groupDao: com.rogger.bp.data.dao.GroupDao
) {

    private var categoryListenerRegistration: ListenerRegistration? = null
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentTargetGroupId: String? = null
    private var currentWorkMode: Int = -1

    fun isSyncing(): Boolean = categoryListenerRegistration != null

    private fun getEffectiveGroupId(): String = if (currentWorkMode == 1) currentTargetGroupId ?: "" else ""

    fun fetchAll(callback: FetchCategoriesCallback, forceRefresh: Boolean = false, workMode: Int = 0, groupId: String? = null) {
        repositoryScope.launch {
            val targetGroupId = if (workMode == 1) groupId else null

            // Se o modo é GRUPO mas não temos groupId ainda, não inicia fetch individual
            if (workMode == 1 && targetGroupId == null) {
                android.util.Log.w("CategoryRepository", "FetchAll: Modo grupo ativo mas groupId ainda não disponível")
                callback.onComplete()
                return@launch
            }

            if (!forceRefresh && categoryListenerRegistration != null && 
                currentTargetGroupId == targetGroupId && currentWorkMode == workMode) {
                callback.onComplete()
                return@launch
            }

            currentTargetGroupId = targetGroupId
            currentWorkMode = workMode

            localCache.getAllCategoriesFlow().firstOrNull()?.let { cached ->
                if (cached.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback.onSuccess(cached)
                    }
                }
            }

            stopListeningForCategories()

            categoryListenerRegistration =
                remoteDataSource.addCategoriesSnapshotListener(
                    targetGroupId,
                    object : FetchCategoriesCallback {
                        override fun onSuccess(categories: List<PostCategory>) {
                            repositoryScope.launch {
                                localCache.replaceAllCategories(categories)
                                callback.onComplete()
                            }
                        }

                        override fun onFailure(message: String) {
                            callback.onFailure(message)
                        }

                        override fun onComplete() {}
                    }
                )
        }
    }

    fun create(category: PostCategory, callback: CategoryCallback) {
        // 1. Salva no cache local do Room imediatamente
        repositoryScope.launch {
            localCache.insertCategory(category)
        }

        // 2. Envia para o Firestore em segundo plano
        remoteDataSource.createCategory(category, callback)

    }

    fun update(category: PostCategory, callback: CategoryCallback) {
        // 1. Atualiza no cache local do Room imediatamente
        repositoryScope.launch {
            localCache.updateCategory(category)
            // ✅ Sincroniza o nome da categoria nos produtos vinculados no Room
            productDao.updateCategoryNameInProducts(category.firestoreId, category.name, getEffectiveGroupId())
        }

        // 2. Envia para o Firestore em segundo plano
        remoteDataSource.updateCategory(category, callback)
    }

    fun delete(category: PostCategory, callback: CategoryCallback) {
        // 1. Remove do cache local imediatamente
        repositoryScope.launch {
            localCache.remove(category.firestoreId)
            // ✅ Limpa a referência da categoria nos produtos vinculados no Room
            productDao.clearCategoryInProducts(category.firestoreId, getEffectiveGroupId())
        }

        // 2. Remove do Firestore
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

    suspend fun clearLocalCache() {
        localCache.clear()
    }

    fun getCachedCategoriesFlow(): Flow<List<PostCategory>> {
        return localCache.getAllCategoriesFlow()
    }

    fun getCachedCategoriesWithCountsFlow(groupId: String): Flow<List<PostCategory>> {
        return localCache.getAllCategoriesFlow().combine(productDao.getAllProducts(groupId)) { categories, products ->
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
