package com.rogger.bp.ui.groups.data

import com.rogger.bp.data.model.PostGroup
import com.rogger.bp.data.model.PostMember
import com.rogger.bp.data.model.PostInvitation
import kotlinx.coroutines.flow.Flow

interface GroupDataSource {
    suspend fun createGroup(group: PostGroup, adminMember: PostMember): Result<Unit>
    suspend fun joinGroup(shareCode: String, member: PostMember): Result<PostGroup>
    suspend fun fetchMembers(groupId: String): Result<List<PostMember>>
    suspend fun fetchUserGroups(userId: String): Result<List<PostGroup>>
    suspend fun findGroupByCode(shareCode: String): Result<PostGroup>
    
    // Invitation methods
    suspend fun sendInvitation(invitation: PostInvitation): Result<Unit>
    fun getInvitationsFlow(userId: String): Flow<List<PostInvitation>>
    fun getGroupInvitationsFlow(groupId: String): Flow<List<PostInvitation>>
    suspend fun respondInvitation(invitation: PostInvitation, accept: Boolean): Result<PostGroup?>
    suspend fun cancelInvitation(invitation: PostInvitation): Result<Unit>
    suspend fun ensureUserGroupExists(userId: String, userName: String, photoUrl: String): Result<PostGroup>
    suspend fun leaveGroup(userId: String, groupId: String): Result<Unit>
    suspend fun syncUserProductsToGroup(userId: String, groupId: String): Result<Unit>
    suspend fun findUserByEmail(email: String): Result<PostMember?>
    suspend fun addMemberToGroup(groupId: String, member: PostMember): Result<Unit>
    suspend fun updateMemberRole(groupId: String, userId: String, newRole: String): Result<Unit>
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
    suspend fun renameGroup(groupId: String, newName: String): Result<Unit>
    suspend fun checkGroupNameExists(adminId: String, name: String): Boolean
    
    // Novas funcionalidades de migração
    suspend fun handleUserLogin(userId: String, userName: String, photoUrl: String): Result<PostGroup>
}
