package com.rogger.bp.ui.home

import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.base.BasePresenter
import com.rogger.bp.ui.base.BaseView

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:40
 */
interface ContractHome {
 interface Presenter : BasePresenter {
  fun fetchProducts()
  fun fetchProductsByCategory(categoryId: String)
  fun deleteProduct(product: PostProduct)
  fun restoreProduct(product: PostProduct)
  /** Busca categorias do Firestore para o dialog do FAB. */
  fun fetchCategories()
 }

 interface View : BaseView<Presenter> {
  fun showProgress(enable: Boolean)
  // fun showProducts(products: List<PostProduct>) // Removido, a View observará o Flow diretamente
  fun showEmpty(isEmpty: Boolean)
  fun onSuccess(name: String)
  fun onError(message: String)
  // fun showCategories(categories: List<PostCategory>) // Removido, a View observará o Flow diretamente
  fun onProductDeleted(product: PostProduct)
  fun onProductRestored(product: PostProduct)
 }
}