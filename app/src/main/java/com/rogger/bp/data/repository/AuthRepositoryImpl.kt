package com.rogger.bp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.rogger.bp.data.model.UserAuth
import com.rogger.bp.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

import com.google.firebase.auth.GoogleAuthProvider

class AuthRepositoryImpl(private val firebaseAuth: FirebaseAuth) : AuthRepository {

    override fun login(email: String, password: String): Flow<Result<UserAuth>> = callbackFlow {
        // ... (existing email/password login remains for now or can be removed if strictly following the request)
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    trySend(Result.success(UserAuth(
                        uuid = user.uid,
                        name = user.displayName ?: "",
                        email = user.email ?: "",
                        password = "",
                        photoUri = user.photoUrl
                    )))
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
                    trySend(Result.success(UserAuth(
                        uuid = user.uid,
                        name = user.displayName ?: "",
                        email = user.email ?: "",
                        password = "",
                        photoUri = user.photoUrl
                    )))
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
            UserAuth(
                uuid = it.uid,
                name = it.displayName ?: "",
                email = it.email ?: "",
                password = "",
                photoUri = it.photoUrl
            )
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}
