package com.rogger.bp.ui.profile.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore

class ProfileRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

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
