package com.rogger.bp.data.model

import com.google.firebase.firestore.PropertyName


data class PostImage(
    @get:PropertyName("barcode")
    @set:PropertyName("barcode")
    var barcode: String = "",

    @get:PropertyName("nomeProduto")
    @set:PropertyName("nomeProduto")
    var name: String = "",

    @get:PropertyName("imagemUrl")
    @set:PropertyName("imagemUrl")
    var uri: String = ""
)
