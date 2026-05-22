package com.rogger.bp.data.model

import android.annotation.SuppressLint
import android.net.Uri

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "products")
data class PostProduct(
    @PrimaryKey
    var firestoreDocId: String = "", // Usado como chave primária no Room, deve ser o documentId do Firestore
    var id: Int = 0,
    var userId: String = "",
    var uuid: String = "", // Manter para compatibilidade, mas firestoreDocId será a chave principal para o cache
    var name: String = "",
    var note: String = "",
    var barcode: String = "",
    var categoryId: Int = 0,
    var timestamp: Long = 0L,
    var imageUri: String = "",
    var deleted: Boolean = false,
    var deletedAt: Long? = null,
    var categoryName: String = "",
    @Ignore val localUri: Uri? = null, // Ignorar para o Room, pois Uri não é um tipo primitivo
    @Ignore val publisher: UserAuth? = null // Ignorar para o Room
) : Parcelable {

    constructor(
        firestoreDocId: String,
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
        firestoreDocId = firestoreDocId,
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