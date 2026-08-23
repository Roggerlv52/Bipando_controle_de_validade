package com.rogger.bp.ui.groups.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rogger.bp.data.model.PostGroup
import com.rogger.bp.data.model.PostMember
import com.rogger.bp.data.model.PostInvitation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FireGroupDataSource : GroupDataSource {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FireGroupDataSource"

    override suspend fun createGroup(group: PostGroup, adminMember: PostMember): Result<Unit> {
        return try {
            val exists = checkGroupNameExists(group.adminId, group.name)
            if (exists) {
                throw Exception("Você já possui um grupo com o nome \"${group.name}\"")
            }

            Log.d(TAG, "Creating group: ${group.groupId} (Default: ${group.isDefault})")
            db.collection("groups").document(group.groupId).set(group).await()

            Log.d(TAG, "Adding admin member: ${adminMember.userId}")
            db.collection("groups").document(group.groupId)
                .collection("members").document(group.adminId).set(adminMember).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating group: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun joinGroup(shareCode: String, member: PostMember): Result<PostGroup> {
        return try {
            Log.d(TAG, "Joining group with code: $shareCode")
            val query = db.collection("groups").whereEqualTo("shareCode", shareCode.uppercase()).get().await()
            if (query.isEmpty) {
                Result.failure(Exception("Grupo não encontrado"))
            } else {
                val doc = query.documents.first()
                val group = doc.toObject(PostGroup::class.java)!!
                
                db.collection("groups").document(group.groupId)
                    .collection("members").document(member.userId).set(member).await()
                    
                Result.success(group)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error joining group: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchMembers(groupId: String): Result<List<PostMember>> {
        return try {
            val snapshot = db.collection("groups").document(groupId)
                .collection("members").get().await()
            val list = snapshot.toObjects(PostMember::class.java)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchUserGroups(userId: String): Result<List<PostGroup>> {
        return try {
            coroutineScope {
                // 1. Busca onde o usuário é o admin
                val adminQueryTask = async {
                    db.collection("groups").whereEqualTo("adminId", userId).get().await()
                }

                // 2. Busca onde ele é MEMBRO convidado (Collection Group Query)
                val memberQueryTask = async {
                    db.collectionGroup("members").whereEqualTo("userId", userId).get().await()
                }

                val adminSnapshot = adminQueryTask.await()
                val memberSnapshot = memberQueryTask.await()

                val groups = adminSnapshot.toObjects(PostGroup::class.java).toMutableList()

                // Busca os documentos de grupo para as participações como membro em paralelo
                val memberGroups = memberSnapshot.documents.map { doc ->
                    async {
                        val groupDocRef = doc.reference.parent.parent
                        if (groupDocRef != null) {
                            val groupDoc = groupDocRef.get().await()
                            val group = groupDoc.toObject(PostGroup::class.java)
                            // Adiciona se não for admin (para não duplicar) e for válido
                            if (group != null && group.adminId != userId) group else null
                        } else null
                    }
                }.awaitAll().filterNotNull()

                groups.addAll(memberGroups)
                Result.success(groups.distinctBy { it.groupId })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchUserGroups: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun checkGroupNameExists(adminId: String, name: String): Boolean {
        return try {
            val query = db.collection("groups")
                .whereEqualTo("adminId", adminId)
                .whereEqualTo("name", name)
                .get().await()
            !query.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun findGroupByCode(shareCode: String): Result<PostGroup> {
        return try {
            val code = shareCode.trim().uppercase()
            val groupQuery = db.collection("groups").whereEqualTo("shareCode", code).get().await()
            
            if (!groupQuery.isEmpty) {
                Result.success(groupQuery.documents.first().toObject(PostGroup::class.java)!!)
            } else {
                val userQuery = db.collection("users").whereEqualTo("shareCode", code).get().await()
                if (userQuery.isEmpty) {
                    Result.failure(Exception("Código inválido ou inexistente"))
                } else {
                    val userDoc = userQuery.documents.first()
                    val targetUid = userDoc.id
                    val name = userDoc.getString("name") ?: "Grupo"
                    val photoUrl = userDoc.getString("photoUrl") ?: ""
                    
                    // PROBLEMA 4 — Corrigido: Busca ou cria um grupo real vinculado a esse usuário
                    ensureUserGroupExists(targetUid, name, photoUrl)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar grupo por código: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun sendInvitation(invitation: PostInvitation): Result<Unit> {
        return try {
            val groupId = invitation.groupId
            if (groupId.isEmpty()) throw Exception("GroupId não pode estar vazio")
            
            val id = db.collection("groups").document(groupId).collection("invitations").document().id
            val inv = invitation.copy(id = id, createdAt = System.currentTimeMillis())
            
            db.collection("groups").document(groupId)
                .collection("invitations").document(id).set(inv).await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * PROBLEMA 6 — Documentado:
     * Para que esta query funcione em produção, é necessário criar um índice composto no Firebase Console:
     * Coleção: invitations (Collection Group)
     * Campos: targetUid (Ascending), status (Ascending)
     */
    override fun getInvitationsFlow(userId: String): Flow<List<PostInvitation>> = callbackFlow {
        val registration = db.collectionGroup("invitations")
            .whereEqualTo("targetUid", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Ignora erro de permissão se for durante logout ou deleção de conta
                    val isLoggingOut = GroupRepository.isLoggingOut()
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED || isLoggingOut) {
                        Log.d(TAG, "Listener de convites interrompido ou sem permissão (isLoggingOut=$isLoggingOut).")
                    } else {
                        Log.e(TAG, "Erro no listener de convites: ${error.message}")
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(PostInvitation::class.java))
                }
            }
        awaitClose { registration.remove() }
    }

    override fun getGroupInvitationsFlow(groupId: String): Flow<List<PostInvitation>> = callbackFlow {
        val registration = db.collection("groups").document(groupId)
            .collection("invitations")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    trySend(snapshot.toObjects(PostInvitation::class.java))
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun respondInvitation(invitation: PostInvitation, accept: Boolean): Result<PostGroup?> {
        return try {
            val status = if (accept) "accepted" else "rejected"
            var joinedGroup: PostGroup? = null
            
            val invitationRef = db.collection("groups").document(invitation.groupId)
                .collection("invitations").document(invitation.id)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(invitationRef)
                if (!snapshot.exists()) throw Exception("Convite não encontrado")
                
                val currentStatus = snapshot.getString("status")
                if (currentStatus != "pending") throw Exception("Convite já respondido ou cancelado")

                transaction.update(invitationRef, mapOf(
                    "status" to status,
                    "respondedAt" to System.currentTimeMillis()
                ))

                if (accept) {
                    val user = FirebaseAuth.getInstance().currentUser ?: throw Exception("Usuário não autenticado")
                    
                    val member = PostMember(
                        userId = invitation.targetUid,
                        name = user.displayName ?: "",
                        email = user.email ?: "",
                        photoUrl = user.photoUrl?.toString() ?: "",
                        role = invitation.role
                    )
                    
                    val memberRef = db.collection("groups").document(invitation.groupId)
                        .collection("members").document(invitation.targetUid)
                    
                    transaction.set(memberRef, member)
                }
            }.await()

            if (accept) {
                val groupDoc = db.collection("groups").document(invitation.groupId).get().await()
                joinedGroup = groupDoc.toObject(PostGroup::class.java)
            }
            
            Result.success(joinedGroup)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao responder convite: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun cancelInvitation(invitation: PostInvitation): Result<Unit> {
        return try {
            db.collection("groups").document(invitation.groupId)
                .collection("invitations").document(invitation.id)
                .update(mapOf(
                    "status" to "cancelled",
                    "respondedAt" to System.currentTimeMillis()
                )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ensureUserGroupExists(userId: String, userName: String, photoUrl: String): Result<PostGroup> {
        return try {
            val existingGroupsResult = fetchUserGroups(userId)
            val existingGroups = existingGroupsResult.getOrDefault(emptyList())
            
            val defaultGroup = existingGroups.find { it.adminId == userId && it.isDefault }
                ?: existingGroups.find { it.adminId == userId }
            
            if (defaultGroup != null) {
                Result.success(defaultGroup)
            } else {
                // PROBLEMA 3 — Corrigido: ID único e campo isDefault
                val newGroupId = db.collection("groups").document().id
                val shortCode = userId.take(4).uppercase() + (1000..9999).random()
                
                val group = PostGroup(
                    groupId = newGroupId,
                    name = "Home",
                    adminId = userId,
                    shareCode = shortCode,
                    createdAt = System.currentTimeMillis(),
                    isDefault = true
                )
                val member = PostMember(
                    userId = userId,
                    name = userName,
                    photoUrl = photoUrl,
                    role = "Admin"
                )
                
                createGroup(group, member)
                Result.success(group)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in ensureUserGroupExists: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun leaveGroup(userId: String, groupId: String): Result<Unit> {
        return try {
            val groupRef = db.collection("groups").document(groupId)
            val groupDoc = groupRef.get().await()
            val group = groupDoc.toObject(PostGroup::class.java)
            
            if (group != null && group.adminId == userId) {
                Log.d(TAG, "Admin leaving. Deleting group and all subcollections: $groupId")
                // PROBLEMA 1 — Corrigido: Deleta subcoleções antes do pai
                coroutineScope {
                    val subcollections = listOf("members", "invitations", "products", "category")
                    subcollections.map { sub ->
                        async { deleteCollection(groupRef.collection(sub)) }
                    }.awaitAll()
                }
                groupRef.delete().await()
            } else {
                groupRef.collection("members").document(userId).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving group: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun deleteCollection(collectionRef: com.google.firebase.firestore.CollectionReference) {
        try {
            val snapshot = collectionRef.get().await()
            if (!snapshot.isEmpty) {
                snapshot.documents.chunked(500).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting collection: ${e.message}")
        }
    }

    override suspend fun syncUserProductsToGroup(userId: String, groupId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Starting syncUserProductsToGroup. User: $userId, Group: $groupId")
            
            // Verifica se o grupo já tem produtos
            val existingProducts = db.collection("groups").document(groupId).collection("products").limit(1).get().await()
            if (!existingProducts.isEmpty) {
                Log.d(TAG, "Sync skipped: Group already has products (Found ${existingProducts.size()}).")
                return Result.success(Unit)
            }
            
            val userProducts = db.collection("users").document(userId).collection("products").get().await()
            val userCategories = db.collection("users").document(userId).collection("category").get().await()
            
            Log.d(TAG, "Found ${userProducts.size()} products and ${userCategories.size()} categories in user collection.")
            
            if (userProducts.isEmpty && userCategories.isEmpty) {
                Log.d(TAG, "No data to sync from user collection.")
                Result.success(Unit)
            } else {
                // Sincroniza Categorias
                if (!userCategories.isEmpty) {
                    Log.d(TAG, "Syncing ${userCategories.size()} categories...")
                    userCategories.documents.chunked(400).forEach { chunk ->
                        val batch = db.batch()
                        chunk.forEach { doc ->
                            val targetRef = db.collection("groups").document(groupId).collection("category").document(doc.id)
                            batch.set(targetRef, doc.data ?: emptyMap<String, Any>(), SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                }
                
                // Sincroniza Produtos
                if (!userProducts.isEmpty) {
                    Log.d(TAG, "Syncing ${userProducts.size()} products...")
                    userProducts.documents.chunked(400).forEach { chunk ->
                        val batch = db.batch()
                        chunk.forEach { doc ->
                            val targetRef = db.collection("groups").document(groupId).collection("products").document(doc.id)
                            val data = doc.data?.toMutableMap() ?: mutableMapOf()
                            data["groupId"] = groupId // Vincula o produto ao novo grupo
                            batch.set(targetRef, data, SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                }
                
                Log.d(TAG, "Sync completed successfully.")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncUserProductsToGroup: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * NOVA FUNCIONALIDADE — Migração segura de dados
     */
    override suspend fun handleUserLogin(userId: String, userName: String, photoUrl: String): Result<PostGroup> = try {
        Log.d(TAG, "Handling login for user: $userId")
        val userDoc = db.collection("users").document(userId).get().await()
        val isMigrated = userDoc.getBoolean("migrated") ?: false

        if (isMigrated) {
            Log.d(TAG, "User already migrated.")
            ensureUserGroupExists(userId, userName, photoUrl)
        } else {
            Log.d(TAG, "Starting migration flow for user: $userId")
            
            // Verifica se tem produtos antigos
            val productsSnapshot = db.collection("users").document(userId).collection("products").limit(1).get().await()
            val hasOldData = !productsSnapshot.isEmpty

            // Busca/Cria grupo padrão
            val groupResult = ensureUserGroupExists(userId, userName, photoUrl)
            val group = groupResult.getOrThrow()

            if (hasOldData) {
                val migrationResult = migrateUserProductsToGroup(userId, group.groupId)
                if (migrationResult.isSuccess) {
                    db.collection("users").document(userId).update("migrated", true).await()
                    Log.d(TAG, "Migration successful and flag set.")
                } else {
                    Log.e(TAG, "Migration failed, flag not set.")
                    throw migrationResult.exceptionOrNull() ?: Exception("Migration failed")
                }
            } else {
                // Sem dados antigos, apenas marca como migrado
                db.collection("users").document(userId).update("migrated", true).await()
                Log.d(TAG, "No old data found. Flag set.")
            }
            Result.success(group)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error in handleUserLogin: ${e.message}")
        Result.failure(e)
    }

    private suspend fun migrateUserProductsToGroup(userId: String, groupId: String): Result<Unit> = try {
        Log.d(TAG, "Migrating products from users/$userId to groups/$groupId")
        
        val userProducts = db.collection("users").document(userId).collection("products").get().await()
        val userCategories = db.collection("users").document(userId).collection("category").get().await()

        // 1. Cópia de Categorias
        if (!userCategories.isEmpty) {
            userCategories.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    val targetRef = db.collection("groups").document(groupId).collection("category").document(doc.id)
                    batch.set(targetRef, doc.data ?: emptyMap<String, Any>())
                }
                batch.commit().await()
            }
        }

        // 2. Cópia de Produtos
        if (!userProducts.isEmpty) {
            userProducts.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    val targetRef = db.collection("groups").document(groupId).collection("products").document(doc.id)
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["groupId"] = groupId
                    batch.set(targetRef, data)
                }
                batch.commit().await()
            }
        }

        // 3. Verificação (Opcional, mas recomendado: verificar se destino não está vazio se origem não estava)
        // Se chegamos aqui sem exceção, os commits foram bem sucedidos.

        // 4. Deleção dos dados antigos
        Log.d(TAG, "Copy successful. Deleting old data...")
        coroutineScope {
            val delProducts = async { deleteCollection(db.collection("users").document(userId).collection("products")) }
            val delCategories = async { deleteCollection(db.collection("users").document(userId).collection("category")) }
            awaitAll(delProducts, delCategories)
        }

        Log.d(TAG, "Migration complete for $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error during migration: ${e.message}")
        Result.failure(e)
    }

    override suspend fun findUserByEmail(email: String): Result<PostMember?> {
        return try {
            val query = db.collection("users").whereEqualTo("email", email.trim().lowercase()).get().await()
            if (query.isEmpty) {
                Result.success(null)
            } else {
                val doc = query.documents.first()
                val member = PostMember(
                    userId = doc.id,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    role = "Reader"
                )
                Result.success(member)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMemberToGroup(groupId: String, member: PostMember): Result<Unit> {
        return try {
            db.collection("groups").document(groupId)
                .collection("members").document(member.userId).set(member).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMemberRole(groupId: String, userId: String, newRole: String): Result<Unit> {
        return try {
            db.collection("groups").document(groupId)
                .collection("members").document(userId).update("role", newRole).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return try {
            db.collection("groups").document(groupId)
                .collection("members").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameGroup(groupId: String, newName: String): Result<Unit> {
        return try {
            db.collection("groups").document(groupId).update("name", newName).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
