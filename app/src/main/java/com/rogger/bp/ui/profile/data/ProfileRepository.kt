package com.rogger.bp.ui.profile.data

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.util.ImagePikerUtil
import com.rogger.bp.util.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
        if (user == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        val uid = user.uid

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Deletar subcoleções do usuário no Firestore
                deleteSubcollection("users/$uid/products")
                deleteSubcollection("users/$uid/category")
                deleteSubcollection("users/$uid/productImages")

                // 2. Se o usuário for dono de um grupo (ID do grupo = UID), removemos o grupo e seus dados
                try {
                    deleteSubcollection("groups/$uid/products")
                    deleteSubcollection("groups/$uid/category")
                    deleteSubcollection("groups/$uid/members")
                    deleteSubcollection("groups/$uid/invitations")
                    firestore.collection("groups").document(uid).delete().await()
                } catch (e: Exception) {
                    Log.w("ProfileRepository", "Erro ao remover grupo próprio: ${e.message}")
                }

                // 3. Remover usuário de outros grupos onde ele é membro
                try {
                    val memberQuery = firestore.collectionGroup("members")
                        .whereEqualTo("userId", uid)
                        .get().await()
                    
                    for (doc in memberQuery.documents) {
                        doc.reference.delete().await()
                    }
                } catch (e: Exception) {
                    Log.w("ProfileRepository", "Erro ao remover de outros grupos: ${e.message}")
                }

                // 3. Deletar imagens no Storage
                // Imagem de perfil
                try {
                    storage.reference.child("profile_images/$uid.jpg").delete().await()
                } catch (e: Exception) { /* Ignora se não existir */ }

                // Imagens privadas de produtos (pasta do usuário)
                try {
                    val productImagesFolder = storage.reference.child("produtos/$uid")
                    val listResult = productImagesFolder.listAll().await()
                    for (item in listResult.items) {
                        item.delete().await()
                    }
                } catch (e: Exception) { /* Ignora se pasta não existir */ }

                // 4. Deletar documento principal do usuário
                firestore.collection("users").document(uid).delete().await()

                // 5. Deletar a conta do Auth (Pode exigir reautenticação se for login antigo)
                user.delete().await()

                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                    callback.onComplete()
                }
            } catch (e: Exception) {
                Log.e("ProfileRepository", "Erro crítico ao deletar conta: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    val errorMessage = if (e is FirebaseAuthRecentLoginRequiredException) {
                        "Por segurança, esta operação requer login recente. Por favor, saia e entre novamente antes de excluir."
                    } else {
                        e.message ?: "Erro ao remover conta e dados"
                    }
                    callback.onFailure(errorMessage)
                    callback.onComplete()
                }
            }
        }
    }

    private suspend fun deleteSubcollection(path: String) {
        try {
            val snapshot = firestore.collection(path).get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.w("ProfileRepository", "Falha ao limpar subcoleção $path: ${e.message}")
        }
    }
}
