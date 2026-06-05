package com.rogger.bp.ui.home.presentation

import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.FetchCategoriesCallback
import com.rogger.bp.ui.home.ContractHome
import com.rogger.bp.ui.home.data.FetchProductsCallback
import com.rogger.bp.ui.home.data.HomeCallback
import com.rogger.bp.ui.home.data.HomeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:34
 */
class HomePresenter(
    private var view: ContractHome.View?,
    private val repository: HomeRepository,
    private val categoryRepository: CategoryRepository
) : ContractHome.Presenter {

    private val _products = MutableStateFlow<List<PostProduct>?>(null)
    val products: StateFlow<List<PostProduct>?> = _products.asStateFlow()

    private val _categories = MutableStateFlow<List<PostCategory>>(emptyList())
    val categories: StateFlow<List<PostCategory>> = _categories.asStateFlow()

    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())
    private var productsCollectionJob: Job? = null
    private var categoriesCollectionJob: Job? = null

    // ── 1. Buscar produtos ────────────────────────────────────────────────

    override fun fetchProducts() {
        // Inicia o listener do Firestore e atualiza o cache local
        repository.fetchAll(object : FetchProductsCallback {
            override fun onSuccess(products: List<PostProduct>) {

            }

            override fun onFailure(message: String) {
                view?.onError(message)
                view?.showProgress(false)
            }

            override fun onComplete() {
                view?.showProgress(false)
                // onComplete não é chamado para listeners contínuos
            }
        })

        // Coleta os produtos do cache local e os expõe para a View
        productsCollectionJob?.cancel() // Cancela o job anterior se houver
        productsCollectionJob = presenterScope.launch {
            repository.getCachedProductsFlow().collectLatest {
                _products.value = it
                view?.showProgress(false)
            }
        }
    }

    override fun fetchProductsByCategory(categoryId: String) {
        if (categoryId.isEmpty()) {
            view?.onError("Categoria inválida")
            return
        }
        view?.showProgress(true)

        filterByCategory(categoryId)
    }

    fun filterByCategory(categoryId: String) {
        productsCollectionJob?.cancel()
        productsCollectionJob = presenterScope.launch {
            if (categoryId.isEmpty()) {
                repository.getCachedProductsFlow()
            } else {
                repository.getCachedProductsByCategoryFlow(categoryId)
            }.collectLatest { products ->
                _products.value = products
            }
        }
    }


    override fun deleteProduct(product: PostProduct) {
        view?.showProgress(true)

        repository.delete(product, object : HomeCallback {
            override fun onSuccess(p: PostProduct) {
                view?.onSuccess("\"${p.name}\" eliminado")
            }

            override fun onFailure(message: String) {
                view?.onError(message)
            }

            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }


    override fun restoreProduct(product: PostProduct) {
        view?.showProgress(true)

        repository.restore(product, object : HomeCallback {
            override fun onSuccess(p: PostProduct) {
                view?.onSuccess("\"${p.name}\" restaurado")
            }

            override fun onFailure(message: String) {
                view?.onError(message)
            }

            override fun onComplete() {
                view?.showProgress(false)
                // Não precisa chamar fetchProducts() aqui, o listener do Firestore e o Flow do Room já farão a atualização
            }
        })
    }

    // ── 5. Buscar categorias (para dialog do FAB) ─────────────────────────

    override fun fetchCategories() {
        // Inicia o listener do Firestore para categorias e atualiza o cache local
        categoryRepository.fetchAll(object : FetchCategoriesCallback {
            override fun onSuccess(categories: List<PostCategory>) {
                // Os dados serão propagados via Flow do Room, não precisamos atualizar a view diretamente aqui.
            }

            override fun onFailure(message: String) {
                view?.onError(message)
            }

            override fun onComplete() {
                // onComplete não é chamado para listeners contínuos
            }
        })

        // Coleta as categorias do cache local e as expõe para a View
        categoriesCollectionJob?.cancel()
        categoriesCollectionJob = presenterScope.launch {
            categoryRepository.getCachedCategoriesFlow().collectLatest {
                _categories.value = it
                // view?.showCategories(it) // A view observará o StateFlow diretamente
            }
        }
    }

    override fun onDestroy() {
        repository.stopListeningForProducts()
        categoryRepository.stopListeningForCategories()
        productsCollectionJob?.cancel()
        categoriesCollectionJob?.cancel()
        presenterScope.cancel()
        view = null
    }
}