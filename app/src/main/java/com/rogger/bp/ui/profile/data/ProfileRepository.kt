package com.rogger.bp.ui.profile.data

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

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
                        val photoUrl = document.getString("photoUrl") ?: ""
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

    fun uploadProfileImage(imageUri: Uri, callback: UploadProfileImageCallback) {
        val user = auth.currentUser
        if (user != null) {
            val storageRef = storage.reference.child("profile_images/${user.uid}.jpg")
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val photoUrl = uri.toString()
                        firestore.collection("users").document(user.uid)
                            .update("photoUrl", photoUrl)
                            .addOnSuccessListener {
                                callback.onSuccess(photoUrl)
                            }
                            .addOnFailureListener { e ->
                                callback.onFailure(e.message ?: "Erro ao atualizar foto no Firestore")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    callback.onFailure(e.message ?: "Erro ao fazer upload da imagem")
                }
                .addOnCompleteListener {
                    callback.onComplete()
                }
        } else {
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
