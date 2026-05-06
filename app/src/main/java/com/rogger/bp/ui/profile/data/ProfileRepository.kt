package com.rogger.bp.ui.profile.data

import com.google.firebase.auth.FirebaseAuth

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
                        val errorMessage = task.exception?.message ?: "Erro desconhecido ao remover conta"
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
