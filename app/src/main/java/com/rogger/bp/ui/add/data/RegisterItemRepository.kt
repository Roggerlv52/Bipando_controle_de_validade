package com.rogger.bp.ui.add.data

import com.rogger.bp.data.database.RoomProductCache
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterItemRepository(
    private val dataSource: ItemDataSource,
    private val localCache: RoomProductCache
) {

    fun create(product: PostProduct, callback: RegisterItemCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            localCache.insertProduct(product)
        }

        dataSource.createItem(product, callback)
        callback.onSuccess(null)
        callback.onComplete()
    }

    fun createImage(image: PostImage, callback: SaveImageCallback) {
        dataSource.saveProductImage(image, callback)
    }

    fun uploadImage(image: PostImage, callback: SaveImageCallback) {
        dataSource.uploadImage(image, callback)
    }
}

// Alias ou Wrapper para compatibilidade com o DependencyInjector
typealias RegisterRepository = RegisterItemRepository
