package com.rogger.bp.ui.groups.data

import com.google.firebase.firestore.FirebaseFirestore
import com.rogger.bp.data.model.PostGroup
import com.rogger.bp.data.model.PostMember
import com.rogger.bp.data.model.PostInvitation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FireGroupDataSource : GroupDataSource {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun createGroup(group: PostGroup, adminMember: PostMember): Result<Unit> = try {
        android.util.Log.d("FireGroupDataSource", "Creating group: ${group.groupId}")
        db.collection("groups").document(group.groupId).set(group).await()

        android.util.Log.d("FireGroupDataSource", "Adding admin member: ${adminMember.userId}")
        db.collection("groups").document(group.groupId)
            .collection("members").document(group.adminId).set(adminMember).await()
            
        Result.success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("FireGroupDataSource", "Error creating group: ${e.message}", e)
        Result.failure(e)
    }

    override suspend fun joinGroup(shareCode: String, member: PostMember): Result<PostGroup> = try {
        android.util.Log.d("FireGroupDataSource", "Joining group with code: $shareCode")
        val query = db.collection("groups").whereEqualTo("shareCode", shareCode).get().await()
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
        android.util.Log.e("FireGroupDataSource", "Error joining group: ${e.message}", e)
        Result.failure(e)
    }

    override suspend fun fetchMembers(groupId: String): Result<List<PostMember>> = try {
        val snapshot = db.collection("groups").document(groupId)
            .collection("members").get().await()
        val list = snapshot.toObjects(PostMember::class.java)
        Result.success(list)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun fetchUserGroup(userId: String): Result<PostGroup?> {
        try {
            // 1. Busca primeiro onde o usuário é o admin (Consulta simples, funciona sem índice extra)
            val adminQuery = db.collection("groups").whereEqualTo("adminId", userId).get().await()
            if (!adminQuery.isEmpty) {
                val group = adminQuery.documents.first().toObject(PostGroup::class.java)
                // Se o admin nomeou o grupo (modo colaborativo), retorna ele.
                if (group != null && group.name != "Meu Grupo") {
                    return Result.success(group)
                }
            }

            // 2. Se não for admin de um grupo nomeado, busca onde ele é MEMBRO convidado (Exige índice)
            val memberQuery = db.collectionGroup("members")
                .whereEqualTo("userId", userId)
                .get().await()
            
            if (!memberQuery.isEmpty) {
                for (doc in memberQuery.documents) {
                    val groupDocRef = doc.reference.parent.parent
                    if (groupDocRef != null) {
                        val groupDoc = groupDocRef.get().await()
                        val group = groupDoc.toObject(PostGroup::class.java)
                        // Retorna o primeiro grupo de outra pessoa que encontrar
                        if (group != null && group.adminId != userId) {
                            return Result.success(group)
                        }
                    }
                }
            }

            // 3. Fallback: Se não encontrou grupo colaborativo, retorna o grupo "Meu Grupo" do admin se existir
            if (!adminQuery.isEmpty) {
                return Result.success(adminQuery.documents.first().toObject(PostGroup::class.java))
            }

        } catch (e: Exception) {
            android.util.Log.e("FireGroupDataSource", "Error in fetchUserGroup: ${e.message}")
            // Se o erro for falta de índice, o app ainda funcionará no modo admin básico
        }
        return Result.success(null)
    }

    override suspend fun findGroupByCode(shareCode: String): Result<PostGroup> = try {
        val code = shareCode.uppercase()
        // 1. Tenta buscar na coleção de grupos (grupos colaborativos nomeados)
        var query = db.collection("groups").whereEqualTo("shareCode", code).get().await()
        
        if (!query.isEmpty) {
            Result.success(query.documents.first().toObject(PostGroup::class.java)!!)
        } else {
            // 2. Fallback: Busca na coleção de usuários (usuários que ainda não criaram um grupo colaborativo)
            val userQuery = db.collection("users").whereEqualTo("shareCode", code).get().await()
            if (userQuery.isEmpty) {
                Result.failure(Exception("Usuário não encontrado com este código"))
            } else {
                val userDoc = userQuery.documents.first()
                val targetUid = userDoc.id
                // Retorna um grupo "virtual" que aponta para o ID do usuário (modo individual dele)
                Result.success(PostGroup(
                    groupId = targetUid,
                    adminId = targetUid,
                    name = userDoc.getString("name") ?: "Meu Grupo",
                    shareCode = code
                ))
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("FireGroupDataSource", "Erro ao buscar grupo/usuário por código: ${e.message}")
        Result.failure(e)
    }

    override suspend fun sendInvitation(invitation: PostInvitation): Result<Unit> = try {
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

    override fun getInvitationsFlow(userId: String): Flow<List<PostInvitation>> = callbackFlow {
        // Usa Collection Group Query para buscar em todos os subdiretórios 'invitations'
        val registration = db.collectionGroup("invitations")
            .whereEqualTo("targetUid", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FireGroupDataSource", "Erro no listener de convites: ${error.message}")
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

    override suspend fun respondInvitation(invitation: PostInvitation, accept: Boolean): Result<PostGroup?> = try {
        val status = if (accept) "accepted" else "rejected"
        var joinedGroup: PostGroup? = null
        
        val invitationRef = db.collection("groups").document(invitation.groupId)
            .collection("invitations").document(invitation.id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(invitationRef)
            if (!snapshot.exists()) throw Exception("Convite não encontrado")
            
            val currentStatus = snapshot.getString("status")
            if (currentStatus != "pending") throw Exception("Convite já respondido ou cancelado")

            val respondedAt = System.currentTimeMillis()
            transaction.update(invitationRef, mapOf(
                "status" to status,
                "respondedAt" to respondedAt
            ))

            if (accept) {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    ?: throw Exception("Usuário não autenticado")
                
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
        android.util.Log.e("FireGroupDataSource", "Erro ao responder convite: ${e.message}")
        Result.failure(e)
    }

    override suspend fun cancelInvitation(invitation: PostInvitation): Result<Unit> = try {
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

    override suspend fun ensureUserGroupExists(userId: String, userName: String, photoUrl: String): Result<PostGroup> = try {
        // Primeiro verifica se o usuário já pertence a ALGUM grupo (como admin ou membro)
        val existingGroupResult = fetchUserGroup(userId)
        val existingGroup = existingGroupResult.getOrNull()
        
        if (existingGroup != null) {
            // Se já existe um grupo, garante que o shareCode esteja no formato correto
            if (existingGroup.shareCode.length > 10) {
                val shortCode = existingGroup.groupId.take(8).uppercase()
                db.collection("groups").document(existingGroup.groupId).update("shareCode", shortCode).await()
                existingGroup.shareCode = shortCode
            }
            Result.success(existingGroup)
        } else {
            // Se realmente não existe nada, cria o grupo padrão (individual)
            val groupId = userId 
            val shortCode = userId.take(8).uppercase()
            val group = PostGroup(
                groupId = groupId,
                name = "Meu Grupo",
                adminId = userId,
                shareCode = shortCode,
                createdAt = System.currentTimeMillis()
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

    override suspend fun leaveGroup(userId: String, groupId: String): Result<Unit> = try {
        val groupDoc = db.collection("groups").document(groupId).get().await()
        val group = groupDoc.toObject(PostGroup::class.java)
        
        if (group != null && group.adminId == userId) {
            db.collection("groups").document(groupId).update("name", "Meu Grupo").await()
        } else {
            db.collection("groups").document(groupId)
                .collection("members").document(userId).delete().await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun syncUserProductsToGroup(userId: String, groupId: String): Result<Unit> = try {
        android.util.Log.d("FireGroupDataSource", "Starting sync. User: $userId, Group: $groupId")
        
        // 1. Busca produtos e categorias do usuário
        val userProducts = db.collection("users").document(userId).collection("products").get().await()
        val userCategories = db.collection("users").document(userId).collection("category").get().await()
        
        android.util.Log.d("FireGroupDataSource", "Sync found ${userProducts.size()} products and ${userCategories.size()} categories")
        
        if (userProducts.isEmpty && userCategories.isEmpty) {
            Result.success(Unit)
        } else {
            // Firestore Batch tem limite de 500 operações. 
            // Para ser seguro, vamos processar em pedaços se necessário, ou apenas usar vários commits.
            
            // Sincroniza Categorias (geralmente poucas)
            if (!userCategories.isEmpty) {
                val catBatch = db.batch()
                userCategories.forEach { catDoc ->
                    val targetRef = db.collection("groups").document(groupId).collection("category").document(catDoc.id)
                    catBatch.set(targetRef, catDoc.data)
                }
                catBatch.commit().await()
                android.util.Log.d("FireGroupDataSource", "Categories synced")
            }
            
            // Sincroniza Produtos (pode ser muitos, vamos usar chunks de 400)
            val productDocs = userProducts.documents
            productDocs.chunked(400).forEach { chunk ->
                val prodBatch = db.batch()
                chunk.forEach { prodDoc ->
                    val targetRef = db.collection("groups").document(groupId).collection("products").document(prodDoc.id)
                    val data = prodDoc.data?.toMutableMap() ?: mutableMapOf()
                    data["groupId"] = groupId
                    // Garante que o ID do produto seja mantido se houver campo 'uuid' ou similar
                    prodBatch.set(targetRef, data)
                }
                prodBatch.commit().await()
            }
            
            android.util.Log.d("FireGroupDataSource", "Products synced successfully")
            Result.success(Unit)
        }
    } catch (e: Exception) {
        android.util.Log.e("FireGroupDataSource", "Critical error in syncUserProductsToGroup: ${e.message}", e)
        Result.failure(e)
    }

    override suspend fun findUserByEmail(email: String): Result<PostMember?> = try {
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

    override suspend fun addMemberToGroup(groupId: String, member: PostMember): Result<Unit> = try {
        db.collection("groups").document(groupId)
            .collection("members").document(member.userId).set(member).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateMemberRole(groupId: String, userId: String, newRole: String): Result<Unit> = try {
        db.collection("groups").document(groupId)
            .collection("members").document(userId).update("role", newRole).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> = try {
        db.collection("groups").document(groupId)
            .collection("members").document(userId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun renameGroup(groupId: String, newName: String): Result<Unit> = try {
        db.collection("groups").document(groupId).update("name", newName).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
