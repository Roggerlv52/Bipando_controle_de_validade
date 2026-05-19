package com.rogger.bp.ui.category.data

import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.model.PostCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class CategoryRepository(
    private val remoteDataSource: PostCategoryDataSource,
    private val localCache: RoomCategoryCache
) {

    private var categoryListenerRegistration: ListenerRegistration? = null

    fun fetchAll(callback: FetchCategoriesCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Tentar carregar do cache local imediatamente
            localCache.getAllCategoriesFlow().firstOrNull()?.let { cachedCategories ->
                if (cachedCategories.isNotEmpty()) {
                    callback.onSuccess(cachedCategories)
                }
            }

            // 2. Configurar listener do Firestore para atualizações em tempo real
            categoryListenerRegistration = remoteDataSource.addCategoriesSnapshotListener(object : FetchCategoriesCallback {
                override fun onSuccess(categories: List<PostCategory>) {
                    // Atualizar o cache local com os dados mais recentes do Firestore
                    CoroutineScope(Dispatchers.IO).launch {
                        localCache.putAllCategories(categories)
                        // A UI já estará observando o Flow do Room, então não precisa chamar callback.onSuccess aqui novamente
                    }
                }

                override fun onFailure(message: String) {
                    callback.onFailure(message)
                }

                override fun onComplete() {
                    // onComplete não é chamado para listeners contínuos
                }
            })
        }
    }

    fun create(category: PostCategory, callback: CategoryCallback) {
        remoteDataSource.createCategory(category, object : CategoryCallback {
            override fun onSuccess(p: PostCategory) {
                CoroutineScope(Dispatchers.IO).launch {
                    localCache.insertCategory(p)
                }
                callback.onSuccess(p)
            }

            override fun onFailure(message: String) {
                callback.onFailure(message)
            }

            override fun onComplete() {
                callback.onComplete()
            }

            override fun onAlreadyExists(existingCategory: PostCategory) {
                callback.onAlreadyExists(existingCategory)
            }
        })
    }

    fun update(category: PostCategory, callback: CategoryCallback) {
        remoteDataSource.updateCategory(category, object : CategoryCallback {
            override fun onSuccess(p: PostCategory) {
                CoroutineScope(Dispatchers.IO).launch {
                    localCache.updateCategory(p)
                }
                callback.onSuccess(p)
            }

            override fun onFailure(message: String) {
                callback.onFailure(message)
            }

            override fun onComplete() {
                callback.onComplete()
            }

            override fun onAlreadyExists(existingCategory: PostCategory) {
                callback.onAlreadyExists(existingCategory)
            }
        })
    }

    fun delete(category: PostCategory, callback: CategoryCallback) {
        remoteDataSource.deleteCategory(category, object : CategoryCallback {
            override fun onSuccess(p: PostCategory) {
                CoroutineScope(Dispatchers.IO).launch {
                    localCache.deleteCategory(p)
                }
                callback.onSuccess(p)
            }

            override fun onFailure(message: String) {
                callback.onFailure(message)
            }

            override fun onComplete() {
                callback.onComplete()
            }

            override fun onAlreadyExists(existingCategory: PostCategory) {
                callback.onAlreadyExists(existingCategory)
            }
        })
    }

    fun stopListeningForCategories() {
        categoryListenerRegistration?.remove()
        categoryListenerRegistration = null
    }

    fun getCachedCategoriesFlow(): Flow<List<PostCategory>> {
        return localCache.getAllCategoriesFlow()
    }
}
