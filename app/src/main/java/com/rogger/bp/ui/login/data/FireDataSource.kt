package com.rogger.bp.ui.login.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rogger.bp.data.model.UserAuth

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 22:39
 */
class FireDataSource : LoginDataSource {

    private val TAG = "FireDataSource"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun login(idToken: String, email: String, callback: LoginCallback) {

        if (idToken.isBlank()) {
            callback.onFailure("Token de autenticação inválido")
            callback.onComplete()
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    val msg = task.exception?.message ?: "Falha na autenticação Google"
                    Log.e(TAG, "signInWithCredential falhou: $msg")
                    callback.onFailure(msg)
                    callback.onComplete()
                    return@addOnCompleteListener
                }

                val user = auth.currentUser
                if (user == null) {
                    callback.onFailure("Utilizador não encontrado após autenticação")
                    callback.onComplete()
                    return@addOnCompleteListener
                }

                val uid = user.uid
                val userName = user.displayName ?: ""
                val photoUrl = user.photoUrl?.toString() ?: ""

                val finalEmail = email

                val firestoreData = hashMapOf(
                    "uid" to uid,
                    "name" to userName,
                    "email" to finalEmail,
                    "photoUrl" to photoUrl
                )
                Log.d("AUTH", auth.currentUser?.uid ?: "NULL")
                firestore.collection("users")
                    .document(uid)
                    .set(firestoreData, SetOptions.merge())
                    .addOnSuccessListener {
                        // ── Devolve UserAuth completo com photoUri ────────
                        val userAuth = UserAuth(
                            uuid = uid,
                            name = userName,
                            email = finalEmail,
                            password = "",
                            photoUri = user.photoUrl   // android.net.Uri directo do FirebaseUser
                        )
                        callback.onSuccess(userAuth)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Erro ao salvar no Firestore: ${exception.message}")
                        val userAuth = UserAuth(
                            uuid = uid,
                            name = userName,
                            email = finalEmail,
                            password = "",
                            photoUri = user.photoUrl
                        )
                        callback.onSuccess(userAuth)
                    }
                    .addOnCompleteListener {
                        callback.onComplete()
                    }
            }
    }

}