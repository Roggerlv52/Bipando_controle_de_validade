package com.rogger.bp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "categories")
data class PostCategory(
    @PrimaryKey
    var firestoreId: String = "", // Usado como chave primária no Room, deve ser o documentId do Firestore
    var name: String = "",
    var userId: String = ""
)