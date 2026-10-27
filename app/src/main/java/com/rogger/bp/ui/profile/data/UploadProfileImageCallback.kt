package com.rogger.bp.ui.profile.data

interface UploadProfileImageCallback {
    fun onSuccess(photoUrl: String)
    fun onFailure(message: String)
    fun onComplete()
}
