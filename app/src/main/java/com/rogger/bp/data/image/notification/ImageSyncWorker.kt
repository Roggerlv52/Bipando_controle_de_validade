package com.rogger.bp.data.image.notification

import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.data.database.BpDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ImageSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val database = BpDatabase.Companion.getDatabase(applicationContext)
        val productDao = database.productDao()

        val allProducts = productDao.getAllCachedProducts()
        if (allProducts.isNullOrEmpty()) {
            return Result.success()
        }

        val pendingProducts = allProducts.filter { product ->
            val uri = product.imageUri
            uri.isNotEmpty() && (uri.startsWith("/") || uri.startsWith("file://")) && !product.deleted
        }

        if (pendingProducts.isEmpty()) {
            return Result.success()
        }

        val storage = FirebaseStorage.getInstance()
        val db = FirebaseFirestore.getInstance()
        var hasFailures = false

        for (product in pendingProducts) {
            try {
                val cleanPath = product.imageUri.replace("file://", "")
                val file = File(cleanPath)

                if (!file.exists()) {
                    continue
                }

                val storagePath = "produtos/$uid/${product.barcode}.jpg"
                val imageRef = storage.reference.child(storagePath)
                val fileUri = Uri.fromFile(file)

                Tasks.await(imageRef.putFile(fileUri))

                val downloadUrl = Tasks.await(imageRef.downloadUrl).toString()

                val updatedProduct = product.copy(imageUri = downloadUrl)

                CoroutineScope(Dispatchers.IO).launch {
                    productDao.updateProduct(updatedProduct)
                }

                val productRef = db.collection("users")
                    .document(uid)
                    .collection("products")
                    .document(product.firestoreDocId)
                Tasks.await(productRef.update("imageUri", downloadUrl))

                if (product.barcode.isNotEmpty()) {
                    val globalRef = db.collection("imageProdutos").document(product.barcode)
                    val globalSnap = Tasks.await(globalRef.get())
                    if (!globalSnap.exists()) {
                        globalRef.set(
                            hashMapOf(
                                "barcode" to product.barcode,
                                "name"    to product.name,
                                "uri"     to downloadUrl
                            )
                        )
                    }
                }

            } catch (_: Exception) {
                hasFailures = true
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }
}
