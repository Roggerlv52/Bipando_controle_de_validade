package com.rogger.bp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.rogger.bp.data.model.UserAuth
import com.rogger.bp.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AuthRepositoryImpl(private val firebaseAuth: FirebaseAuth) : AuthRepository {

    private fun saveUserToFirestore(user: FirebaseUser, onComplete: () -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val userRef = firestore.collection("users").document(user.uid)
        
        // Tenta obter o e-mail de forma mais robusta percorrendo os provedores se o principal for nulo
        var email = user.email
        if (email.isNullOrEmpty()) {
            for (profile in user.providerData) {
                if (!profile.email.isNullOrEmpty()) {
                    email = profile.email
                    break
                }
            }
        }
        val finalEmail = email ?: ""

        userRef.get().addOnSuccessListener { document ->
            val data = hashMapOf<String, Any>(
                "uid" to user.uid,
                "name" to (user.displayName ?: ""),
                "email" to finalEmail,
                "shareCode" to user.uid.take(8).uppercase() // Adiciona o código de 8 dígitos ao perfil
            )
            
            if (!document.exists()) {
                // Novo usuário: inicializa campos padrão
                data["isPremium"] = false
                data["photoUrl"] = user.photoUrl?.toString() ?: ""
            } else {
                // Usuário existente: preserva isPremium e só atualiza foto se estiver vazia no Firestore
                val currentPhoto = document.getString("photoUrl")
                if (currentPhoto.isNullOrEmpty()) {
                    data["photoUrl"] = user.photoUrl?.toString() ?: ""
                }
                
                // Se o e-mail for vazio (falhou ao pegar agora), mas já existe no banco, não sobrescreve com vazio
                val existingEmail = document.getString("email")
                if (finalEmail.isEmpty() && !existingEmail.isNullOrEmpty()) {
                    data["email"] = existingEmail
                }
            }
            
            userRef.set(data, SetOptions.merge())
                .addOnCompleteListener { onComplete() }
        }.addOnFailureListener {
            onComplete()
        }
    }

    override fun login(email: String, password: String): Flow<Result<UserAuth>> = callbackFlow {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    saveUserToFirestore(user) {
                        trySend(Result.success(UserAuth(
                            uuid = user.uid,
                            name = user.displayName ?: "",
                            email = user.email ?: email, // Usa o email do parâmetro se o do user for null
                            password = "",
                            photoUri = user.photoUrl
                        )))
                    }
                } else {
                    trySend(Result.failure(Exception("Usuário nulo")))
                }
            }
            .addOnFailureListener {
                trySend(Result.failure(it))
            }
        awaitClose()
    }

    override fun loginWithGoogle(idToken: String): Flow<Result<UserAuth>> = callbackFlow {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    saveUserToFirestore(user) {
                        // Busca o email de forma robusta para o retorno também
                        val finalEmail = user.email ?: user.providerData.firstOrNull { !it.email.isNullOrEmpty() }?.email ?: ""
                        
                        trySend(Result.success(UserAuth(
                            uuid = user.uid,
                            name = user.displayName ?: "",
                            email = finalEmail,
                            password = "",
                            photoUri = user.photoUrl
                        )))
                    }
                } else {
                    trySend(Result.failure(Exception("Usuário nulo após login Google")))
                }
            }
            .addOnFailureListener {
                trySend(Result.failure(it))
            }
        awaitClose()
    }

    override fun getCurrentUser(): UserAuth? {
        val user = firebaseAuth.currentUser
        return user?.let {
            val finalEmail = it.email ?: it.providerData.firstOrNull { !it.email.isNullOrEmpty() }?.email ?: ""
            UserAuth(
                uuid = it.uid,
                name = it.displayName ?: "",
                email = finalEmail,
                password = "",
                photoUri = it.photoUrl
            )
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}
