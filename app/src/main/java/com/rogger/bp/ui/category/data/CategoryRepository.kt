package com.rogger.bp.ui.category.data

import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.model.PostCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class CategoryRepository(
    private val remoteDataSource: PostCategoryDataSource,
    private val localCache: RoomCategoryCache
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
        remoteDataSource.createCategory(category, object : CategoryCallback {
            override fun onSuccess(p: PostCategory) {
                callback.onSuccess(p)
            }

            override fun onFailure(message: String) = callback.onFailure(message)
            override fun onComplete() = callback.onComplete()
            override fun onAlreadyExists(existingCategory: PostCategory) =
                callback.onAlreadyExists(existingCategory)
        })
    }

    fun update(category: PostCategory, callback: CategoryCallback) {
        remoteDataSource.updateCategory(category, object : CategoryCallback {
            override fun onSuccess(p: PostCategory) {
                callback.onSuccess(p)
            }

            override fun onFailure(message: String) = callback.onFailure(message)
            override fun onComplete() = callback.onComplete()
            override fun onAlreadyExists(existingCategory: PostCategory) =
                callback.onAlreadyExists(existingCategory)
        })
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
    fun destroy() {
        stopListeningForCategories()
        repositoryScope.cancel()
    }
}