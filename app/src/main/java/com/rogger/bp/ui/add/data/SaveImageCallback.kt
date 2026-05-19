package com.rogger.bp.ui.add.data

import com.rogger.bp.data.model.PostImage

interface SaveImageCallback {
    fun onSuccess(image: PostImage)
    fun onAlreadyExists(image: PostImage)
    fun onFailure(message: String)
    fun onComplete()
}