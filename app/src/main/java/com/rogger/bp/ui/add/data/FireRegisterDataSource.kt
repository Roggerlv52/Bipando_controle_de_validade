package com.rogger.bp.ui.add.data

import android.util.Log
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
                    // Se não existe no Firestore, mas o usuário passou uma URI local, faz upload
                    if (image.uri.isNotEmpty() && image.uri.startsWith("file")) {
                        uploadImage(image, callback)
                    } else {
                        // Apenas informamos que não foi encontrado para liberar a UI
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

    override fun uploadImage(
        image: PostImage,
        callback: SaveImageCallback
    ) {
        val storage = FirebaseStorage.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Usando o padrão de pasta: imagens_produtos/{barcode}/imagem.jpg
        val imageRef = storage.reference
            .child("imagens_produtos/${image.barcode}/imagem.jpg")

        val fileUri = image.uri.toUri()
        Log.d("FireRegister", "Iniciando upload de: $fileUri")

        imageRef.putFile(fileUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        val downloadUrl = downloadUri.toString()
                        Log.d("FireRegister", "Upload concluído. URL: $downloadUrl")

                        val productImage = PostImage(
                            barcode = image.barcode,
                            name = image.name,
                            uri = downloadUrl // Agora salvamos a URL de download (https://...)
                        )

                        firestore
                            .collection("imagens_produtos")
                            .document(image.barcode)
                            .set(productImage)
                            .addOnSuccessListener {
                                callback.onSuccess()
                            }
                            .addOnFailureListener { exception ->
                                callback.onFailure(
                                    exception.message ?: "Erro ao salvar metadados no Firestore"
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        callback.onFailure(
                            exception.message ?: "Erro ao obter URL de download"
                        )
                    }
            }
            .addOnFailureListener { exception ->
                callback.onFailure(
                    exception.message ?: "Erro no upload da imagem para o Storage"
                )
            }
            .addOnCompleteListener {
                callback.onComplete()
            }
    }
}