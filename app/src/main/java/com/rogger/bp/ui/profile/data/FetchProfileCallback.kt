package com.rogger.bp.ui.profile.data

interface FetchProfileCallback {
    fun onSuccess(name: String, email: String, photoUrl: String, isPremium: Boolean)
    fun onFailure(message: String)
    fun onComplete()
}
