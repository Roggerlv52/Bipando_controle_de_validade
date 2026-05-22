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
        // Cancela qualquer listener anterior antes de criar um novo.
        // Sem isso, cada vez que fetchAll() é chamado (ex: ao voltar para o Fragment)
        // acumula-se um listener extra, e cada mudança no Firestore dispara N vezes
        // para o Room, causando N emissões do Flow e duplicação visual.
        stopListeningForCategories()

        CoroutineScope(Dispatchers.IO).launch {
            // 1. Carregar do cache local imediatamente (offline-first)
            localCache.getAllCategoriesFlow().firstOrNull()?.let { cachedCategories ->
                if (cachedCategories.isNotEmpty()) {
                    callback.onSuccess(cachedCategories)
                }
            }

            // 2. Configurar UM ÚNICO listener do Firestore para atualizações em tempo real
            categoryListenerRegistration = remoteDataSource.addCategoriesSnapshotListener(object : FetchCategoriesCallback {
                override fun onSuccess(categories: List<PostCategory>) {
                    CoroutineScope(Dispatchers.IO).launch {
                        // Usa replaceAllCategories (clear + insertAll) em vez de putAllCategories
                        // (INSERT OR REPLACE puro). Isso garante que categorias removidas no
                        // Firestore também sejam removidas do Room, e que o Flow emita apenas
                        // UMA vez com o estado final correto.
                        localCache.replaceAllCategories(categories)
                    }
                }

                override fun onFailure(message: String) {
                    callback.onFailure(message)
                }

                override fun onComplete() {
                    // Não chamado para listeners contínuos
                }
            })
        }
    }

    fun create(category: PostCategory, callback: CategoryCallback) {
        remoteDataSource.createCategory(category, object : CategoryCallback {
            override fun onSuccess(p: PostCategory) {
                // Não inserir manualmente no cache aqui.
                // O snapshot listener já atualiza o Room via replaceAllCategories.
                callback.onSuccess(p)
            }
            override fun onFailure(message: String) = callback.onFailure(message)
            override fun onComplete() = callback.onComplete()
            override fun onAlreadyExists(existingCategory: PostCategory) = callback.onAlreadyExists(existingCategory)
        })
    }

    fun update(category: PostCategory, callback: CategoryCallback) {
        remoteDataSource.updateCategory(category, object : CategoryCallback {
            override fun onSuccess(p: PostCategory) {
                // Não atualizar manualmente no cache aqui.
                // O snapshot listener já atualiza o Room via replaceAllCategories.
                callback.onSuccess(p)
            }
            override fun onFailure(message: String) = callback.onFailure(message)
            override fun onComplete() = callback.onComplete()
            override fun onAlreadyExists(existingCategory: PostCategory) = callback.onAlreadyExists(existingCategory)
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
            override fun onAlreadyExists(existingCategory: PostCategory) = callback.onAlreadyExists(existingCategory)
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