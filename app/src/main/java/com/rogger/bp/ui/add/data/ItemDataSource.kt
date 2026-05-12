package com.rogger.bp.ui.add.data

import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct

interface ItemDataSource {

    fun createItem(product: PostProduct, callback: RegisterItemCallback)
    fun saveProductImage(image: PostImage, callback: SaveImageCallback)
    fun uploadImage(image: PostImage, callback: SaveImageCallback)

}