package com.rogger.bp.ui.profile.data

interface UpdateProfileCallback {
    fun onSuccess()
    fun onFailure(message: String)
    fun onComplete()
}
