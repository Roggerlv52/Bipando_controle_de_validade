package com.rogger.bp.ui.groups.data

import com.rogger.bp.data.dao.GroupDao
import com.rogger.bp.data.model.PostGroup
import com.rogger.bp.data.model.PostMember
import com.rogger.bp.data.model.PostInvitation
import kotlinx.coroutines.flow.Flow

class GroupRepository(
    private val dataSource: GroupDataSource,
    private val groupDao: GroupDao
) {
    fun getLocalGroupFlow(): Flow<PostGroup?> = groupDao.getGroupFlow()

    suspend fun createGroup(group: PostGroup, adminMember: PostMember): Result<Unit> {
        val result = dataSource.createGroup(group, adminMember)
        if (result.isSuccess) {
            groupDao.clearGroup()
            groupDao.insertGroup(group)
        }
        return result
    }

    suspend fun fetchMembers(groupId: String): Result<List<PostMember>> {
        return dataSource.fetchMembers(groupId)
    }

    suspend fun joinGroup(shareCode: String, member: PostMember): Result<PostGroup> {
        val result = dataSource.joinGroup(shareCode, member)
        result.onSuccess { group ->
            groupDao.clearGroup()
            groupDao.insertGroup(group)
        }
        return result
    }

    suspend fun syncUserGroup(userId: String) {
        val result = dataSource.fetchUserGroup(userId)
        result.onSuccess { group ->
            val currentGroup = groupDao.getGroup()
            if (group != null) {
                // Só atualiza se for um grupo diferente ou se houve mudança real
                if (currentGroup?.groupId != group.groupId || currentGroup.name != group.name || currentGroup.shareCode != group.shareCode) {
                    groupDao.clearGroup()
                    groupDao.insertGroup(group)
                }
            } else if (currentGroup != null) {
                // Se o usuário não pertence mais a nenhum grupo, limpa o cache
                groupDao.clearGroup()
            }
        }
    }

    suspend fun findGroupByCode(shareCode: String): Result<PostGroup> {
        return dataSource.findGroupByCode(shareCode)
    }

    suspend fun sendInvitation(invitation: PostInvitation): Result<Unit> {
        return dataSource.sendInvitation(invitation)
    }

    fun getInvitationsFlow(userId: String): Flow<List<PostInvitation>> {
        return dataSource.getInvitationsFlow(userId)
    }

    fun getGroupInvitationsFlow(groupId: String): Flow<List<PostInvitation>> {
        return dataSource.getGroupInvitationsFlow(groupId)
    }

    suspend fun respondInvitation(invitation: PostInvitation, accept: Boolean): Result<Unit> {
        val result = dataSource.respondInvitation(invitation, accept)
        result.onSuccess { group ->
            if (group != null) {
                groupDao.clearGroup()
                groupDao.insertGroup(group)
            }
        }
        return if (result.isSuccess) Result.success(Unit) else Result.failure(result.exceptionOrNull()!!)
    }

    suspend fun cancelInvitation(invitation: PostInvitation): Result<Unit> {
        return dataSource.cancelInvitation(invitation)
    }

    suspend fun ensureUserGroupExists(userId: String, userName: String, photoUrl: String): Result<PostGroup> {
        val result = dataSource.ensureUserGroupExists(userId, userName, photoUrl)
        result.onSuccess { group ->
            groupDao.insertGroup(group)
        }
        return result
    }

    suspend fun leaveGroup(userId: String, groupId: String): Result<Unit> {
        val result = dataSource.leaveGroup(userId, groupId)
        if (result.isSuccess) {
            groupDao.clearGroup()
        }
        return result
    }

    suspend fun clearLocalCache() {
        groupDao.clearGroup()
    }

    suspend fun syncUserToGroup(userId: String, groupId: String): Result<Unit> {
        return dataSource.syncUserProductsToGroup(userId, groupId)
    }

    suspend fun findUserByEmail(email: String): Result<PostMember?> {
        return dataSource.findUserByEmail(email)
    }

    suspend fun addMemberToGroup(groupId: String, member: PostMember): Result<Unit> {
        return dataSource.addMemberToGroup(groupId, member)
    }

    suspend fun updateMemberRole(groupId: String, userId: String, newRole: String): Result<Unit> {
        return dataSource.updateMemberRole(groupId, userId, newRole)
    }

    suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return dataSource.removeMember(groupId, userId)
    }

    suspend fun renameGroup(groupId: String, newName: String): Result<Unit> {
        val result = dataSource.renameGroup(groupId, newName)
        if (result.isSuccess) {
            val currentGroup = groupDao.getGroup()
            if (currentGroup != null) {
                groupDao.insertGroup(currentGroup.copy(name = newName))
            }
        }
        return result
    }
}
