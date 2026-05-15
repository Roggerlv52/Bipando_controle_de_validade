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
    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun login(email: String, name: String, callback: LoginCallback) {

        val idToken = email   // renomeado por clareza

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

                val uid       = user.uid
                val userName  = user.displayName ?: ""
                val userEmail = user.email ?: ""
                val photoUrl  = user.photoUrl?.toString() ?: ""

                Log.d(TAG, "Firebase Auth OK — uid=$uid name=$userName email=$userEmail")

                // ── Persiste dados no Firestore ───────────────────────────
                // ✅ Verificação: Se o e-mail do FirebaseUser for vazio, tenta usar o que veio do Google se disponível
                val finalEmail = if (userEmail.isNotBlank()) userEmail else email
                
                val firestoreData = hashMapOf(
                    "uid"      to uid,
                    "name"     to userName,
                    "email"    to finalEmail,
                    "photoUrl" to photoUrl
                )

                firestore.collection("users")
                    .document(uid)
                    .set(firestoreData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "Dados salvos no Firestore: uid=$uid email=$finalEmail")

                        // ── Devolve UserAuth completo com photoUri ────────
                        val userAuth = UserAuth(
                            uuid     = uid,
                            name     = userName,
                            email    = finalEmail,
                            password = "",
                            photoUri = user.photoUrl   // android.net.Uri directo do FirebaseUser
                        )
                        callback.onSuccess(userAuth)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Erro ao salvar no Firestore: ${exception.message}")
                        // Auth foi bem-sucedida — devolve UserAuth mesmo com erro no Firestore
                        // para não bloquear o utilizador, mas notifica o erro
                        val userAuth = UserAuth(
                            uuid     = uid,
                            name     = userName,
                            email    = finalEmail,
                            password = "",
                            photoUri = user.photoUrl
                        )
                        callback.onSuccess(userAuth)
                        Log.w(TAG, "Login OK mas Firestore falhou: ${exception.message}")
                    }
                    .addOnCompleteListener {
                        callback.onComplete()
                    }
            }
    }


    private fun saveUserToFirestore(
        uid: String,
        userName: String,
        userEmail: String,
        callback: LoginCallback
    ) {
        val userData = mapOf(
            "uid"   to uid,
            "name"  to userName,
            "email" to userEmail
        )

        firestore.collection("users")
            .document(uid)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Utilizador salvo no Firestore: uid=$uid")

                val userAuth = UserAuth(
                    uuid     = uid,
                    name     = userName,
                    email    = userEmail,
                    password = "",
                    photoUri = auth.currentUser?.photoUrl
                )
                callback.onSuccess(userAuth)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Erro ao salvar no Firestore: ${exception.message}")
                // Autenticação já foi feita com sucesso — não bloqueia o login
                // mas notifica o erro para o Presenter decidir
                callback.onFailure(exception.message ?: "Erro ao salvar dados do utilizador")
            }
            .addOnCompleteListener {
                callback.onComplete()
            }
    }
}
