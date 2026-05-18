package com.rogger.bp.ui.deleteitem.data

import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.category.data.CategoryCallback

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 18/05/2026
 * Hora: 09:38
 */
interface PostDeletedItem {
    fun restoreItemDeleted(item: PostProduct,callback: DeleteItemCallback)
    fun deletePermanently(product: PostProduct, callback: DeleteItemCallback)
    fun fetchItemDeleted(callback: DeleteItemCallback)
}