package com.rogger.bp.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "groups")
data class PostGroup(
    @PrimaryKey
    var groupId: String = "",
    var name: String = "",
    var adminId: String = "",
    var shareCode: String = "",
    var createdAt: Long = 0L,
    var isDefault: Boolean = false
) : Parcelable

@Parcelize
data class PostMember(
    var userId: String = "",
    var name: String = "",
    var email: String = "",
    var photoUrl: String = "",
    var role: String = "Reader" // "Admin" or "Reader"
) : Parcelable
