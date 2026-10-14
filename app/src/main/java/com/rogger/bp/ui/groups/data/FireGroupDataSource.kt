package com.rogger.bp.ui.groups.data

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

    override suspend fun createGroup(group: PostGroup, adminMember: PostMember): Result<Unit> {
        return try {
            val exists = checkGroupNameExists(group.adminId, group.name)
            if (exists) {
                throw Exception("Você já possui um grupo com o nome \"${group.name}\"")
            }

            db.collection("groups").document(group.groupId).set(group).await()

            db.collection("groups").document(group.groupId)
                .collection("members").document(group.adminId).set(adminMember).await()
            
            // Registra o código do grupo na coleção 'shareCodes' para busca direta sem PERMISSION_DENIED
            if (group.shareCode.isNotEmpty()) {
                val shareData = hashMapOf(
                    "type" to "group",
                    "groupId" to group.groupId,
                    "adminId" to group.adminId,
                    "name" to group.name
                )
                try {
                    db.collection("shareCodes").document(group.shareCode.uppercase()).set(shareData, SetOptions.merge())
                } catch (_: Exception) {}
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinGroup(shareCode: String, member: PostMember): Result<PostGroup> {
        return try {
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
                        try {
                            val groupDocRef = doc.reference.parent.parent
                            if (groupDocRef != null) {
                                val groupDoc = groupDocRef.get().await()
                                val group = groupDoc.toObject(PostGroup::class.java)
                                // Adiciona se não for admin (para não duplicar) e for válido
                                if (group != null && group.adminId != userId) group else null
                            } else null
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()

                groups.addAll(memberGroups)
                Result.success(groups.distinctBy { it.groupId })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkGroupNameExists(adminId: String, name: String): Boolean {
        val query = db.collection("groups")
            .whereEqualTo("adminId", adminId)
            .whereEqualTo("name", name)
            .get().await()
        return !query.isEmpty
    }

    override suspend fun findGroupByCode(shareCode: String): Result<PostGroup> {
        return try {
            val code = shareCode.trim().uppercase()

            // 1. Busca direta na coleção 'shareCodes' por ID do documento
            try {
                val shareDoc = db.collection("shareCodes").document(code).get().await()
                if (shareDoc.exists()) {
                    val type = shareDoc.getString("type")
                    val name = shareDoc.getString("name") ?: "Grupo"

                    if (type == "group") {
                        val groupId = shareDoc.getString("groupId") ?: code
                        val adminId = shareDoc.getString("adminId") ?: ""
                        return Result.success(PostGroup(
                            groupId = groupId,
                            adminId = adminId,
                            name = name,
                            shareCode = code
                        ))
                    } else {
                        val targetUid = shareDoc.getString("targetUid") ?: shareDoc.getString("userId") ?: shareDoc.id
                        return Result.success(PostGroup(
                            groupId = "placeholder",
                            adminId = targetUid,
                            name = name,
                            shareCode = code
                        ))
                    }
                }
            } catch (_: Exception) {}

            // 2. Busca na coleção 'groups' por 'shareCode' tratando erros de permissão silenciosamente
            val groupQuery = try {
                db.collection("groups").whereEqualTo("shareCode", code).get().await()
            } catch (_: Exception) {
                null
            }

            if (groupQuery != null && !groupQuery.isEmpty) {
                val group = groupQuery.documents.first().toObject(PostGroup::class.java)!!
                return Result.success(group)
            }

            // 3. Busca na coleção 'users' por 'shareCode' tratando erros de permissão silenciosamente
            val userQuery = try {
                db.collection("users").whereEqualTo("shareCode", code).get().await()
            } catch (_: Exception) {
                null
            }

            if (userQuery != null && !userQuery.isEmpty) {
                val userDoc = userQuery.documents.first()
                val targetUid = userDoc.id
                val name = userDoc.getString("name") ?: "Grupo"

                return Result.success(PostGroup(
                    groupId = "placeholder",
                    adminId = targetUid,
                    name = name,
                    shareCode = code
                ))
            }

            // 4. Caso o código fornecido seja os primeiros 8 caracteres do UID ou o próprio ID do usuário em 'users'
            try {
                val userDocDirect = db.collection("users").document(code).get().await()
                if (userDocDirect.exists()) {
                    val targetUid = userDocDirect.id
                    val name = userDocDirect.getString("name") ?: "Grupo"
                    return Result.success(PostGroup(
                        groupId = "placeholder",
                        adminId = targetUid,
                        name = name,
                        shareCode = code
                    ))
                }
            } catch (_: Exception) {}

            Result.failure(Exception("Usuário não encontrado com este código"))
        } catch (_: Exception) {
            Result.failure(Exception("Usuário não encontrado com este código"))
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
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val invites = snapshot.toObjects(PostInvitation::class.java)
                    trySend(invites)
                }
            }
        awaitClose { 
            registration.remove() 
        }
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
            // Garante que o código de 8 dígitos do usuário esteja registrado na coleção pública 'shareCodes'
            val userShareCode = userId.take(8).uppercase()
            try {
                val shareData = hashMapOf(
                    "type" to "user",
                    "targetUid" to userId,
                    "name" to userName
                )
                db.collection("shareCodes").document(userShareCode).set(shareData, SetOptions.merge())
            } catch (_: Exception) {}

            val existingGroupsResult = fetchUserGroups(userId)
            if (existingGroupsResult.isFailure) {
                return Result.failure(existingGroupsResult.exceptionOrNull() ?: Exception("Falha ao buscar grupos existentes"))
            }
            
            val existingGroups = existingGroupsResult.getOrThrow()
            
            var defaultGroup = existingGroups.find { it.adminId == userId && it.isDefault }
                ?: existingGroups.find { it.adminId == userId }
            
            if (defaultGroup != null) {
                // Se o nome ainda for "Home", atualiza para o nome do usuário
                if (defaultGroup.name == "Home" && userName.isNotEmpty()) {
                    renameGroup(defaultGroup.groupId, userName)
                    defaultGroup = defaultGroup.copy(name = userName)
                }
                Result.success(defaultGroup)
            } else {
                // PROBLEMA 3 — Corrigido: ID único e campo isDefault
                val newGroupId = db.collection("groups").document().id
                val shortCode = userId.take(4).uppercase() + (1000..9999).random()
                
                val group = PostGroup(
                    groupId = newGroupId,
                    name = userName.ifEmpty { "Meu Grupo" },
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
            Result.failure(e)
        }
    }

    override suspend fun leaveGroup(userId: String, groupId: String): Result<Unit> {
        return try {
            val groupRef = db.collection("groups").document(groupId)
            val groupDoc = groupRef.get().await()
            val group = groupDoc.toObject(PostGroup::class.java)
            
            if (group != null && group.adminId == userId) {
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
        } catch (_: Exception) {}
    }

    override suspend fun syncUserProductsToGroup(userId: String, groupId: String): Result<Unit> {
        return try {
            val userProducts = db.collection("users").document(userId).collection("products").get().await()
            val userCategories = db.collection("users").document(userId).collection("category").get().await()
            
            if (userProducts.isEmpty && userCategories.isEmpty) {
                Result.success(Unit)
            } else {
                // Sincroniza Categorias
                if (!userCategories.isEmpty) {
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
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * NOVA FUNCIONALIDADE — Migração segura de dados
     */
    override suspend fun handleUserLogin(userId: String, userName: String, photoUrl: String): Result<PostGroup> = try {
        val userDoc = db.collection("users").document(userId).get().await()
        val isMigrated = userDoc.getBoolean("migrated") ?: false

        if (isMigrated) {
            ensureUserGroupExists(userId, userName, photoUrl)
        } else {
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
                } else {
                    throw migrationResult.exceptionOrNull() ?: Exception("Migration failed")
                }
            } else {
                // Sem dados antigos, apenas marca como migrado
                db.collection("users").document(userId).update("migrated", true).await()
            }
            Result.success(group)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun migrateUserProductsToGroup(userId: String, groupId: String): Result<Unit> = try {
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

        // 4. Deleção dos dados antigos
        coroutineScope {
            val delProducts = async { deleteCollection(db.collection("users").document(userId).collection("products")) }
            val delCategories = async { deleteCollection(db.collection("users").document(userId).collection("category")) }
            awaitAll(delProducts, delCategories)
        }

        Result.success(Unit)
    } catch (e: Exception) {
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
