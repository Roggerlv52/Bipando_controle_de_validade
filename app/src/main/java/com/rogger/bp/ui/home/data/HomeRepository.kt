package com.rogger.bp.ui.home.data

import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:33
 */
 class HomeRepository(private val dataSource: PostHomeDataSource) {

  /**
   * Busca todos os produtos activos do utilizador.
   */
  fun fetchAll(callback: Any) {
   dataSource.fetchProducts(callback)
  }

  /**
   * Busca produtos filtrados por [categoryId].
   */
  fun fetchByCategory(categoryId: Int, callback: FetchProductsCallback) {
   dataSource.fetchProductsByCategory(categoryId, callback)
  }

  /**
   * Soft-delete: marca [product] como eliminado no Firestore.
   */
  fun delete(product: PostProduct, callback: HomeCallback) {
   dataSource.deleteProduct(product, callback)
  }

  /**
   * Restaura um produto previamente eliminado.
   */
  fun restore(product: PostProduct, callback: HomeCallback) {
   dataSource.restoreProduct(product, callback)
  }
 }
