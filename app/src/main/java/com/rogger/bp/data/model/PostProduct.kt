package com.rogger.bp.data.model

import android.net.Uri

data class PostProduct(
    val uuid: String,
    val uri: Uri,
    val name: String,
    val note: String,
    val barcode :String,
    val categoryId: Int,
    val deleted: Boolean,
    val timestamp: Long,
    val publisher: UserAuth
)