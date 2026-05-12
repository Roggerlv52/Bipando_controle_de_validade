package com.rogger.bp.ui.add.data

import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct

class FireRegisterDataSource : ItemDataSource {
    override fun createItem(
        produto: PostProduct,
        callback: RegisterItemCallback
    ) {

        val uid = FirebaseAuth.getInstance().uid

        if (uid != null) {

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("products")
                .document()
                .set(
                    hashMapOf(
                        "uuid" to produto.uuid,
                        "imageUri" to produto.uri,
                        "name" to produto.name,
                        "note" to produto.note,
                        "barcode" to produto.barcode,
                        "categoryId" to produto.categoryId,
                        "deleted" to produto.deleted,
                        "timestamp" to produto.timestamp,
                    )
                )
                .addOnSuccessListener {
                    callback.onSuccess(null)
                }
                .addOnFailureListener { exception ->
                    callback.onFailure(exception.message.toString())
                }
                .addOnCompleteListener {
                    callback.onComplete()
                }

        } else {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
        }
    }
    override fun saveProductImage(
        image: PostImage,
        callback: SaveImageCallback
    ) {
        val docRef = FirebaseFirestore.getInstance()
            .collection("product_images")
            .document(image.barcode)

        docRef.get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.exists()) {
                    val existing = snapshot.toObject(PostImage::class.java)
                    callback.onAlreadyExists(existing!!)
                } else {
                    uploadImage(image, callback)
                }
            }
            .addOnFailureListener { exception ->
                callback.onFailure(exception.message ?: "Erro")
            }
            .addOnCompleteListener {
                callback.onComplete()
            }
    }

    override fun uploadImage(
        image: PostImage,
        callback: SaveImageCallback
    ) {

        val storage = FirebaseStorage.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val imageRef = storage.reference
            .child("product_images/${image.barcode}.jpg")
        val fileUri = image.uri.toString().toUri()

        imageRef.putFile(fileUri)
            .addOnSuccessListener {

                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->

                        val productImage = PostImage(
                            barcode = image.barcode,
                            name = image.name,
                            uri = downloadUri.toString()
                        )
                        firestore
                            .collection("product_images")
                            .document(image.barcode)
                            .set(productImage)
                            .addOnSuccessListener {
                                callback.onSuccess()
                            }
                            .addOnFailureListener { exception ->

                                callback.onFailure(
                                    exception.message ?: "Erro ao salvar imagem no Firestore"
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        callback.onFailure(
                            exception.message ?: "Erro ao obter URL da imagem"
                        )
                    }
            }
            .addOnFailureListener { exception ->
                callback.onFailure(
                    exception.message ?: "Erro no upload da imagem"
                )
            }
            .addOnCompleteListener {
                callback.onComplete()
            }
    }
}