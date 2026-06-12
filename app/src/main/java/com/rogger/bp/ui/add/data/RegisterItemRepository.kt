package com.rogger.bp.ui.add.data

import com.rogger.bp.data.database.RoomProductCache
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.commun.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterItemRepository(
    private val dataSource: ItemDataSource,
    private val localCache: RoomProductCache
) {

    fun create(product: PostProduct, callback: RegisterItemCallback) {
        val isOnline = NetworkUtils.isNetworkAvailable()

        if (isOnline) {
            // ── CENÁRIO ONLINE ──────────────────────────────────────────────
            // Se estamos online, não fazemos gravação dupla. Esperamos o fluxo normal
            // do Firestore salvar e depois persistimos no Room local com a URL pública.
            dataSource.createItem(product, object : RegisterItemCallback {
                override fun onSuccess(image: PostImage?) {
                    CoroutineScope(Dispatchers.IO).launch {
                        localCache.insertProduct(product)
                    }
                    callback.onSuccess(image)
                }

                override fun onFailure(message: String) {
                    callback.onFailure(message)
                }

                override fun onComplete() {
                    callback.onComplete()
                }
            })
        } else {
            // ── CENÁRIO OFFLINE ─────────────────────────────────────────────
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    callback.onSuccess(null)
                   // callback.onComplete()
                    localCache.insertProduct(product)
                }
            }
        }
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
