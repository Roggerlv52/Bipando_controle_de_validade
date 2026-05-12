package com.rogger.bp.ui.add.data

import com.rogger.bp.data.model.PostImage

interface RegisterItemCallback {
    fun onSuccess(image: PostImage?)
    fun onFailure(message : String)
    fun onComplete()
}