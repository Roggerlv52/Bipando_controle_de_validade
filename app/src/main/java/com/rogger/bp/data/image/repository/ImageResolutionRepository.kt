package com.rogger.bp.data.image.repository

import android.content.Context
import com.rogger.bp.data.image.ImageResult
import com.rogger.bp.data.image.UploadResult
import com.rogger.bp.data.image.datasource.GlobalImageDataSource
import com.rogger.bp.data.image.datasource.UserImageDataSource

class ImageResolutionRepository(
    private val userImageDataSource: UserImageDataSource,
    private val globalImageDataSource: GlobalImageDataSource
) {

    suspend fun resolveImage(barcode: String): ImageResult {
        if (barcode.isBlank()) {
            return ImageResult.NoImage
        }

        val trimmedBarcode = barcode.trim()

        try {
            val userResult = userImageDataSource.fetchUserImage(trimmedBarcode)
            if (userResult is ImageResult.CustomImage) {
                return userResult
            }
        } catch (_: Exception) {}

        val globalResult = globalImageDataSource.fetchGlobalImage(trimmedBarcode)

        if (globalResult is ImageResult.GlobalImage) {
            return globalResult
        }

        if (globalResult is ImageResult.Error) {
            return globalResult
        }

        return ImageResult.NoImage
    }

    suspend fun uploadGlobalImage(
        context: Context,
        barcode: String,
        productName: String,
        imageUri: String
    ): UploadResult {
        return globalImageDataSource.createGlobalImageIfAbsent(context, barcode, productName, imageUri)
    }

    suspend fun saveUserImage(context: Context, barcode: String, imageUri: String): UploadResult {
        return userImageDataSource.saveUserImage(context, barcode, imageUri)
    }

    suspend fun removeUserImage(barcode: String): Boolean {
        return userImageDataSource.removeUserImage(barcode)
    }

    suspend fun globalImageExists(barcode: String): Boolean {
        val result = globalImageDataSource.fetchGlobalImage(barcode)
        return result is ImageResult.GlobalImage
    }
}
