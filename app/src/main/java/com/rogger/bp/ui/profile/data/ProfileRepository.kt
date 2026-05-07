package com.rogger.bp.ui.profile.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException

class ProfileRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun deleteUserAccount(callback: DeleteAccountCallback) {
        val user = auth.currentUser
        
        if (user != null) {
            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback.onSuccess()
                    } else {
                        val exception = task.exception
                        val errorMessage = when (exception) {
                            is FirebaseAuthRecentLoginRequiredException -> {
                                "Por segurança, esta operação requer um login recente. Por favor, saia e entre novamente no aplicativo para deletar sua conta."
                            }
                            else -> exception?.message ?: "Erro desconhecido ao remover conta"
                        }
                        callback.onFailure(errorMessage)
                    }
                    callback.onComplete()
                }
        } else {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
        }
    }
}
