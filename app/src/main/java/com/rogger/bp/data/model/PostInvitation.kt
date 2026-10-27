package com.rogger.bp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostInvitation(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhoto: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val targetUid: String = "",
    val targetEmail: String = "",
    val role: String = "Reader",
    val status: String = "pending", // "pending", "accepted", "rejected", "cancelled"
    val timestamp: Long = System.currentTimeMillis(), // Deprecated, use createdAt
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val respondedAt: Long? = null
) : Parcelable
