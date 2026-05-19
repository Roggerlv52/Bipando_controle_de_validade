package com.rogger.bp.ui.add.data

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import java.io.File

class FireRegisterDataSource : ItemDataSource {
    override fun createItem(
        produto: PostProduct,
        callback: RegisterItemCallback
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("products")
                .document()
                .set(
                    hashMapOf(
                        "uid" to produto.uuid,
                        "imageUri" to produto.imageUri,
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
            .collection("imagens_produtos")
            .document(image.barcode)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val existing = snapshot.toObject(PostImage::class.java)
                    if (existing != null) {
                        callback.onAlreadyExists(existing)
                    } else {
                        callback.onFailure("Erro ao converter dados do produto")
                    }
                } else {

                    if (image.uri.isNotEmpty()) {
                        uploadImage(image, callback)
                    } else {
                        callback.onComplete()
                    }
                }
            }
            .addOnFailureListener { exception ->
                callback.onFailure(exception.message ?: "Erro ao buscar produto")
            }
            .addOnCompleteListener {
                // O onComplete aqui pode ser redundante se disparar upload,
                // mas mantemos para garantir que o progress pare se nada for feito.
                // callback.onComplete() // Removido para não fechar o progress antes do upload
            }
    }

    //8718951006188
    override fun uploadImage(
        image: PostImage,
        callback: SaveImageCallback
    ) {
        val storage = FirebaseStorage.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Usando o padrão de pasta: imagens_produtos/{barcode}/imagem.jpg
        val imageRef = storage.reference
            .child("imagens_produtos/${image.barcode}/imagem.jpg")

        val fileUri: Uri = when {
            image.uri.startsWith("content://") -> Uri.parse(image.uri)
            image.uri.startsWith("file://") -> Uri.parse(image.uri)
            else -> Uri.fromFile(File(image.uri)) // path absoluto → file://
        }

        Log.d("FireRegister", "Iniciando upload de: $fileUri")

        // Uma única chamada putFile (antes havia chamada duplicada que fazia upload duplo)
        imageRef.putFile(fileUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        val productImage = PostImage(
                            barcode = image.barcode,
                            name    = image.name,   // nome digitado pelo utilizador
                            uri     = downloadUri.toString()
                        )
                        firestore.collection("imagens_produtos")
                            .document(image.barcode)
                            .set(productImage)
                            .addOnSuccessListener {
                                callback.onSuccess(productImage)
                                callback.onComplete() // ← aqui
                            }
                            .addOnFailureListener { exception ->
                                callback.onFailure(exception.message ?: "Erro ao salvar metadados")
                                callback.onComplete() // ← e aqui
                            }
                    }
                    .addOnFailureListener { exception ->
                        callback.onFailure(exception.message ?: "Erro ao obter URL")
                        callback.onComplete() // ← e aqui
                    }
            }
            .addOnFailureListener { exception ->
                callback.onFailure(exception.message ?: "Erro no upload")
                callback.onComplete() // ← e aqui
            }
    }
}