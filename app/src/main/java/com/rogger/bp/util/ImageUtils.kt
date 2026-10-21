package com.rogger.bp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utilitário para processamento de imagens antes do upload.
 * Otimizado para reduzir o uso de memória e evitar erros de Canvas grande.
 */
object ImageUtils {
    private const val MAX_DIMENSION = 720
    private const val MAX_FILE_SIZE_BYTES = 80 * 1024
    private const val INITIAL_QUALITY = 85
    private const val MIN_QUALITY = 30
    private const val QUALITY_STEP = 10

    @Throws(Exception::class)
    fun processImage(context: Context, uri: Uri, outputFile: File): File {
        // 1. Decode com subamostragem (evita OOM)
        var bitmap = decodeSampledBitmap(context, uri, MAX_DIMENSION, MAX_DIMENSION)
            ?: throw IOException("Não foi possível decodificar a imagem")

        // 2. Redimensionamento preciso (se necessário)
        bitmap = resizeIfNeeded(bitmap)

        // 3. Rotação baseada em EXIF
        bitmap = rotateIfNeeded(context, bitmap, uri)

        // 4. Compressão adaptativa (foca em tamanho de arquivo)
        compressAdaptively(bitmap, outputFile)
        
        // 5. Liberação imediata de memória
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }

        return outputFile
    }

    private fun compressAdaptively(bitmap: Bitmap, outputFile: File) {
        var quality = INITIAL_QUALITY
        val bos = ByteArrayOutputStream()

        do {
            bos.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            if (bos.size() <= MAX_FILE_SIZE_BYTES) break
            quality -= QUALITY_STEP
        } while (quality >= MIN_QUALITY)

        FileOutputStream(outputFile).use { fos ->
            fos.write(bos.toByteArray())
            fos.flush()
        }
    }

    private fun resizeIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) return bitmap

        val scale = if (width >= height) {
            MAX_DIMENSION.toFloat() / width
        } else {
            MAX_DIMENSION.toFloat() / height
        }

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        // Primeira passagem: apenas lê as dimensões
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
        
        // Calcula o fator de subamostragem
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        
        // Segunda passagem: decodificação real com subamostragem
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateIfNeeded(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    else -> return bitmap
                }
                
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                }
                rotated
            } ?: bitmap
        } catch (_: Exception) {
            bitmap
        }
    }
}
