package com.rogger.bp.ui.home.presentation

import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.FetchCategoriesCallback
import com.rogger.bp.ui.home.ContractHome
import com.rogger.bp.ui.home.data.FetchProductsCallback
import com.rogger.bp.ui.home.data.HomeCallback
import com.rogger.bp.ui.home.data.HomeRepository

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

 // ── 1. Buscar produtos ────────────────────────────────────────────────

 override fun fetchProducts() {
  view?.showProgress(true)

  repository.fetchAll(object : FetchProductsCallback {
   override fun onSuccess(products: List<PostProduct>) {
    view?.showProducts(products)
    view?.showEmpty(products.isEmpty())
   }
   override fun onFailure(message: String) {
    view?.onError(message)
    view?.showEmpty(true)
   }
   override fun onComplete() {
    view?.showProgress(false)
   }
  })
 }

 // ── 2. Buscar produtos por categoria ──────────────────────────────────

 override fun fetchProductsByCategory(categoryId: Int) {
  if (categoryId <= 0) {
   view?.onError("Categoria inválida")
   return
  }
  view?.showProgress(true)

  repository.fetchByCategory(categoryId, object : FetchProductsCallback {
   override fun onSuccess(products: List<PostProduct>) {
    view?.showProducts(products)
    view?.showEmpty(products.isEmpty())
   }
   override fun onFailure(message: String) {
    view?.onError(message)
    view?.showEmpty(true)
   }
   override fun onComplete() {
    view?.showProgress(false)
   }
  })
 }

 // ── 3. Soft-delete ────────────────────────────────────────────────────

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
    fetchProducts()
   }
  })
 }

 // ── 4. Restaurar produto ──────────────────────────────────────────────

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
    fetchProducts()
   }
  })
 }

 // ── 5. Buscar categorias (para dialog do FAB) ─────────────────────────

 override fun fetchCategories() {
  categoryRepository.fetchAll(object : FetchCategoriesCallback {
   override fun onSuccess(categories: List<PostCategory>) {
    view?.showCategories(categories)
   }
   override fun onFailure(message: String) {
    // Sem categorias: entrega lista vazia — o dialog trata o fluxo B
    view?.showCategories(emptyList())
   }
   override fun onComplete() { /* sem progress — chamada silenciosa */ }
  })
 }

 // ── Lifecycle ─────────────────────────────────────────────────────────

 override fun onDestroy() {
  view = null
 }
}