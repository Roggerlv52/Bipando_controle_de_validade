package com.rogger.bp.ui.profile.data

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.util.ImagePikerUtil
import com.rogger.bp.util.ImageUtils

class ProfileRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    fun getUserProfile(callback: FetchProfileCallback) {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: ""
                        val email = document.getString("email") ?: ""
                        // Tenta pegar a foto do Firestore. Se estiver vazia, usa a do Google Auth como fallback.
                        var photoUrl = document.getString("photoUrl") ?: ""
                        if (photoUrl.isEmpty()) {
                            photoUrl = user.photoUrl?.toString() ?: ""
                        }

                        val isPremium = document.getBoolean("isPremium") ?: false
                        callback.onSuccess(name, email, photoUrl, isPremium)
                    } else {
                        callback.onFailure("Perfil não encontrado no Firestore")
                    }
                }
                .addOnFailureListener { e ->
                    callback.onFailure(e.message ?: "Erro ao carregar dados do Firestore")
                }
                .addOnCompleteListener {
                    callback.onComplete()
                }
        } else {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
        }
    }

    fun updateUserName(newName: String, callback: UpdateProfileCallback) {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("users").document(user.uid)
                .update("name", newName)
                .addOnSuccessListener {
                    callback.onSuccess()
                }
                .addOnFailureListener { e ->
                    callback.onFailure(e.message ?: "Erro ao atualizar nome no Firestore")
                }
                .addOnCompleteListener {
                    callback.onComplete()
                }
        } else {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
        }
    }

    fun uploadProfileImage(context: android.content.Context, imageUri: Uri, callback: UploadProfileImageCallback) {
        val user = auth.currentUser
        if (user != null) {
            Log.d("ProfileRepository", "Iniciando processamento de imagem para UID: ${user.uid}")
            try {
                val processedFile = ImageUtils.processImage(context, imageUri, ImagePikerUtil.createImageFile(context))
                val processedUri = Uri.fromFile(processedFile)

                val storageRef = storage.reference.child("profile_images/${user.uid}.jpg")
                val metadata = com.google.firebase.storage.storageMetadata {
                    contentType = "image/jpeg"
                }

                storageRef.putFile(processedUri, metadata)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            val photoUrl = uri.toString()
                            firestore.collection("users").document(user.uid)
                                .update("photoUrl", photoUrl)
                                .addOnSuccessListener {
                                    Log.d("ProfileRepository", "Firestore atualizado com sucesso!")
                                    callback.onSuccess(photoUrl)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ProfileRepository", "Erro ao atualizar Firestore: ${e.message}")
                                    callback.onFailure(e.message ?: "Erro ao atualizar foto no Firestore")
                                }
                        }
                        // Deleta o arquivo processado após o upload
                        ImagePikerUtil.cleanUpTempFiles(processedFile)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileRepository", "Erro no upload para o Storage: ${e.message}")
                        callback.onFailure(e.message ?: "Erro ao fazer upload da imagem")
                        ImagePikerUtil.cleanUpTempFiles(processedFile)
                    }
                    .addOnCompleteListener {
                        callback.onComplete()
                    }
            } catch (e: Exception) {
                Log.e("ProfileRepository", "Erro crítico no processamento/upload: ${e.message}")
                callback.onFailure("Erro ao processar imagem: ${e.message}")
                callback.onComplete()
            }
        } else {
            Log.e("ProfileRepository", "Tentativa de upload sem usuário autenticado.")
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
        }
    }

    fun updatePremiumStatus(isPremium: Boolean, callback: UpdateProfileCallback) {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("users").document(user.uid)
                .update("isPremium", isPremium)
                .addOnSuccessListener {
                    callback.onSuccess()
                }
                .addOnFailureListener { e ->
                    callback.onFailure(e.message ?: "Erro ao atualizar status Premium")
                }
                .addOnCompleteListener {
                    callback.onComplete()
                }
        } else {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
        }
    }

    fun deleteUserAccount(callback: DeleteAccountCallback) {

        val user = auth.currentUser

        if (user != null) {

            val uid = user.uid

            // 🔥 Remove dados do Firestore primeiro
            firestore.collection("users")
                .document(uid)
                .delete()
                .addOnSuccessListener {

                    // 🔥 Depois remove autenticação
                    user.delete()
                        .addOnCompleteListener { task ->

                            if (task.isSuccessful) {

                                callback.onSuccess()

                            } else {

                                val exception = task.exception

                                val errorMessage = when (exception) {

                                    is FirebaseAuthRecentLoginRequiredException -> {
                                        Log.e("DeleteUser","Error:" +exception.toString())
                                        "Por segurança, esta operação requer login recente."
                                    }

                                    else -> {
                                        exception?.message
                                            ?: "Erro ao remover conta"
                                    }
                                }

                                callback.onFailure(errorMessage)
                            }

                            callback.onComplete()
                        }

                }
                .addOnFailureListener { e ->
                    Log.e("Profile", e.toString())
                    callback.onFailure(
                        e.message ?: "Erro ao remover dados do usuário"
                    )

                    callback.onComplete()
                }

        } else {

            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
        }
    }
}
