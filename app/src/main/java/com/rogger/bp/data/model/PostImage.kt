package com.rogger.bp.data.model

import com.google.firebase.firestore.PropertyName


data class PostImage(
    @get:PropertyName("barcode")
    @set:PropertyName("barcode")
    var barcode: String = "",

    @get:PropertyName("nomeProduto")
    @set:PropertyName("nomeProduto")
    var name: String = "",

    @get:PropertyName("imageUri")
    @set:PropertyName("imageUri")
    var uri: String = ""
)
