package com.rogger.bp.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey


@Entity(tableName = "categories")
data class PostCategory(
    @PrimaryKey
    var firestoreId: String = "", // Usado como chave primária no Room
    var name: String = "",
    var userId: String = ""
) {
    @Ignore
    var itemCount: Int = 0 // 👉 Ignorado pelo Room na tabela, calculado sob demanda em memória
}