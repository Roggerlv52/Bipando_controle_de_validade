package com.rogger.bp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


data class PostCategory(
    val id: Int = 0,
    val name: String = "",
    val userId: String = "",
    val count: Int = 0
)