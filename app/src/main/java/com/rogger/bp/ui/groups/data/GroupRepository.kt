package com.rogger.bp.ui.groups.data

import com.rogger.bp.data.dao.GroupDao
import com.rogger.bp.data.model.PostGroup
import com.rogger.bp.data.model.PostMember
import com.rogger.bp.data.model.PostInvitation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GroupRepository(
    private val dataSource: GroupDataSource,
    private val groupDao: GroupDao
) {
    companion object {
        @Volatile
        private var isSystemLoggingOut = false
        
        fun setLoggingOut(value: Boolean) {
            isSystemLoggingOut = value
        }
        
        fun isLoggingOut(): Boolean = isSystemLoggingOut
    }

    fun getLocalGroupsFlow(): Flow<List<PostGroup>> = groupDao.getGroupsFlow()

    fun getLocalGroupFlow(): Flow<PostGroup?> = getLocalGroupsFlow().map { it.firstOrNull() }

    suspend fun getGroupById(groupId: String): PostGroup? = groupDao.getGroupById(groupId)

    suspend fun createGroup(group: PostGroup, adminMember: PostMember): Result<Unit> {
        val result = dataSource.createGroup(group, adminMember)
        if (result.isSuccess) {
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
            groupDao.insertGroup(group)
        }
        return result
    }

    suspend fun syncUserGroup(userId: String) {
        val result = dataSource.fetchUserGroups(userId)
        result.onSuccess { groups ->
            if (groups.isNotEmpty()) {
                groupDao.clearGroup()
                groupDao.insertGroups(groups)
            } else {
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
            groupDao.deleteGroup(groupId)
        }
        return result
    }

    suspend fun clearLocalCache() {
        groupDao.clearGroup()
    }

    suspend fun handleUserLogin(userId: String, userName: String, photoUrl: String): Result<PostGroup> {
        return dataSource.handleUserLogin(userId, userName, photoUrl)
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
            val currentGroup = groupDao.getGroupById(groupId)
            if (currentGroup != null) {
                groupDao.insertGroup(currentGroup.copy(name = newName))
            }
        }
        return result
    }
}
