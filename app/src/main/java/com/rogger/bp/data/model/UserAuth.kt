package com.rogger.bp.data.model

import android.net.Uri

data class UserAuth (
    val uuid: String,
    val name : String,
    val email: String,
    val password: String,
    val photoUri: Uri?,
)