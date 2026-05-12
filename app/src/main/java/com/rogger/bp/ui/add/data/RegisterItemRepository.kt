package com.rogger.bp.ui.add.data

import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct

class RegisterItemRepository(private val dataSource: ItemDataSource) {

    fun create(product: PostProduct, callback: RegisterItemCallback) {
        dataSource.createItem(product, callback)
    }
    fun createImage(image : PostImage,callback: SaveImageCallback){
        dataSource.saveProductImage(image,callback)
    }
    fun uploadImage(image: PostImage,callback: SaveImageCallback){
        dataSource.uploadImage(image,callback)
    }
}