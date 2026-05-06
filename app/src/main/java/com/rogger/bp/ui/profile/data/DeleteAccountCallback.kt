package com.rogger.bp.ui.profile.data

interface DeleteAccountCallback {
    fun onFailure(message: String)
    fun onSuccess()
    fun onComplete()
}
