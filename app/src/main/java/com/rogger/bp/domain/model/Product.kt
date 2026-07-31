package com.rogger.bp.domain.model

data class Product(
    val uuid: String = "",
    val name: String = "",
    val barcode: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val imageUri: String = "",
    val timestamp: Long = 0,
    val note: String = "",
    val deleted: Boolean = false
)
