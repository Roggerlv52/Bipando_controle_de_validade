package com.rogger.bp.data.model

import android.net.Uri
import androidx.room.Ignore

data class PostProduct(

    var id: Int = 0,
    var userId: String = "",
    val uuid: String = "",
    var name: String = "",
    var note: String = "",
    var barcode: String = "",
    var count: Int = 0,
    var categoryId: Int = 0,
    var timestamp: Long = 0L,
    var imageUri: String = "",
    var deleted: Boolean = false,
    var deletedAt: Long? = null,
    val categoryName: String = "",
    val localUri: Uri? = null,
    val publisher: UserAuth? = null
) {
    /**
     * Construtor secundário sem os campos @Ignore — necessário para o Room
     * reconstruir entidades a partir do banco sem os campos ignorados.
     */
    constructor(
        id: Int,
        userId: String,
        uuid: String,
        name: String,
        note: String,
        barcode: String,
        categoryId: Int,
        categoryName: String,
        timestamp: Long,
        imageUri: String,
        deleted: Boolean,
        deletedAt: Long?
    ) : this(
        id = id,
        userId = userId,
        uuid = uuid,
        name = name,
        note = note,
        barcode = barcode,
        categoryId = categoryId,
        categoryName = categoryName,
        timestamp = timestamp,
        imageUri = imageUri,
        deleted = deleted,
        deletedAt = deletedAt,
        localUri = null,
        publisher = null
    )
}
