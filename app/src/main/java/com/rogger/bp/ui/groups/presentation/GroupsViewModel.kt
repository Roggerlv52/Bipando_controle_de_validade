package com.rogger.bp.ui.groups.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rogger.bp.data.model.PostGroup
import com.rogger.bp.data.model.PostMember
import com.rogger.bp.data.model.PostInvitation
import com.rogger.bp.domain.repository.AuthRepository
import com.rogger.bp.ui.commun.SharedPreferencesManager
import android.content.Context
import com.rogger.bp.ui.groups.data.GroupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import com.rogger.bp.data.dao.ProductDao
import androidx.lifecycle.asFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class GroupMember(
    val id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val role: String = "Reader" // "Admin", "Editor", "Reader"
)

data class GroupsState(
    val hasGroup: Boolean = false,
    val groups: List<PostGroup> = emptyList(),
    val groupId: String = "",
    val groupName: String = "",
    val groupCode: String = "",
    val membersMap: Map<String, List<GroupMember>> = emptyMap(),
    val invitations: List<PostInvitation> = emptyList(),
    val activeItemsCount: Int = 0,
    val activeItemsCountMap: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val userPhoto: String = "",
    val userName: String = "",
    val userRole: String = "Admin",
    val workMode: Int = 0, // 0 = Individual, 1 = Grupo
    val showCreateDialog: Boolean = false,
    val showJoinDialog: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModel(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val productDao: ProductDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsState())
    val uiState: StateFlow<GroupsState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeGroups()
        observeInvitations()
    }

    private val countJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    private fun observeCountForGroup(groupId: String) {
        if (countJobs.containsKey(groupId)) return
        
        countJobs[groupId] = productDao.getActiveProductsCountLiveData(groupId).asFlow()
            .onEach { count ->
                _uiState.update { state ->
                    val newMap = state.activeItemsCountMap.toMutableMap()
                    newMap[groupId] = count ?: 0
                    state.copy(activeItemsCountMap = newMap)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeGroups() {
        groupRepository.getLocalGroupsFlow().onEach { groups ->
            val isCollaborative = groups.any {
                it.name != "Home"
            }
            _uiState.update { 
                it.copy(
                    hasGroup = isCollaborative,
                    groups = groups
                )
            }
            // Fetch members and count for each group
            groups.forEach { group ->
                fetchMembers(group.groupId)
                observeCountForGroup(group.groupId)
            }
        }.launchIn(viewModelScope)
    }

    private fun observeInvitations() {
        val user = authRepository.getCurrentUser() ?: return
        groupRepository.getInvitationsFlow(user.uuid).onEach { invites ->
            _uiState.update { it.copy(invitations = invites) }
        }.launchIn(viewModelScope)
    }

    private fun fetchMembers(groupId: String) {
        val currentUser = authRepository.getCurrentUser()
        viewModelScope.launch {
            val result = groupRepository.fetchMembers(groupId)
            result.onSuccess { members ->
                val myRole = members.find { it.userId == currentUser?.uuid }?.role ?: "Admin"
                _uiState.update { state ->
                    val newMap = state.membersMap.toMutableMap()
                    newMap[groupId] = members.map { 
                        GroupMember(it.userId, it.name, it.photoUrl, it.role)
                    }
                    state.copy(
                        userRole = myRole,
                        membersMap = newMap
                    )
                }
            }
        }
    }

    private fun loadInitialData() {
        val user = authRepository.getCurrentUser()
        val userId = user?.uuid ?: ""
        _uiState.update { 
            it.copy(
                userPhoto = user?.photoUri?.toString() ?: "",
                userName = user?.name ?: "",
                isLoading = true 
            )
        }
        
        viewModelScope.launch {
            groupRepository.syncUserGroup(userId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadWorkMode(context: Context) {
        val mode = SharedPreferencesManager.getWorkMode(context)
        val activeGroupId = SharedPreferencesManager.getActiveGroupId(context)
        _uiState.update { it.copy(workMode = mode, groupId = activeGroupId) }
    }

    fun onWorkModeChange(context: Context, mode: Int, groupId: String = "") {
        SharedPreferencesManager.setWorkMode(context, mode)
        if (groupId.isNotEmpty()) {
            SharedPreferencesManager.setActiveGroupId(context, groupId)
        }
        _uiState.update { it.copy(workMode = mode, groupId = if (groupId.isNotEmpty()) groupId else it.groupId) }
    }

    fun createGroup(context: Context, name: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "O nome do grupo não pode estar vazio") }
            return
        }
        
        val user = authRepository.getCurrentUser()
        if (user == null) {
            _uiState.update { it.copy(error = "Usuário não autenticado") }
            return
        }

        val groupId = UUID.randomUUID().toString()
        val shortCode = groupId.take(8).uppercase()
        
        val newGroup = PostGroup(
            groupId = groupId,
            name = name,
            adminId = user.uuid,
            shareCode = shortCode,
            createdAt = System.currentTimeMillis()
        )

        val adminMember = PostMember(
            userId = user.uuid,
            name = user.name,
            email = user.email,
            photoUrl = user.photoUri?.toString() ?: "",
            role = "Admin"
        )

        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            val result = groupRepository.createGroup(newGroup, adminMember)
            if (result.isSuccess) {
                // Tenta sincronizar dados do usuário para o novo grupo criado
                val syncResult = groupRepository.syncUserToGroup(user.uuid, groupId)
                if (syncResult.isSuccess) {
                    android.util.Log.d("GroupsViewModel", "Data sync to new group successful.")
                } else {
                    android.util.Log.e("GroupsViewModel", "Data sync failed: ${syncResult.exceptionOrNull()?.message}")
                }

                SharedPreferencesManager.setWorkMode(context, 1)
                SharedPreferencesManager.setActiveGroupId(context, groupId)
                _uiState.update { it.copy(showCreateDialog = false, hasGroup = true, workMode = 1, groupId = groupId) }
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun joinGroup(context: Context, code: String) {
        if (code.isBlank()) return
        val user = authRepository.getCurrentUser() ?: return
        
        val member = PostMember(
            userId = user.uuid,
            name = user.name,
            email = user.email,
            photoUrl = user.photoUri?.toString() ?: "",
            role = "Reader"
        )

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = groupRepository.joinGroup(code, member)
            result.onSuccess { group ->
                SharedPreferencesManager.setWorkMode(context, 1)
                SharedPreferencesManager.setActiveGroupId(context, group.groupId)
                _uiState.update { it.copy(showJoinDialog = false, hasGroup = true, workMode = 1, groupId = group.groupId) }
            }
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun sendInvitation(groupId: String, targetCode: String, role: String) {
        if (targetCode.isBlank()) return
        val user = authRepository.getCurrentUser() ?: return
        val currentState = _uiState.value
        val group = currentState.groups.find { it.groupId == groupId } ?: return
        
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            val result = groupRepository.findGroupByCode(targetCode.uppercase())
            
            result.onSuccess { targetGroup ->
                val invitation = PostInvitation(
                    senderId = user.uuid,
                    senderName = user.name,
                    senderPhoto = user.photoUri?.toString() ?: "",
                    groupId = group.groupId,
                    groupName = group.name,
                    targetUid = targetGroup.adminId,
                    role = role,
                    status = "pending",
                    createdAt = System.currentTimeMillis()
                )
                groupRepository.sendInvitation(invitation)
                _uiState.update { it.copy(isLoading = false, error = null) }
            }
            
            result.onFailure {
                _uiState.update { it.copy(isLoading = false, error = "Usuário não encontrado com este código") }
            }
        }
    }

    fun respondInvitation(invitation: PostInvitation, accept: Boolean) {
        viewModelScope.launch {
            groupRepository.respondInvitation(invitation, accept)
        }
    }

    fun updateMemberRole(groupId: String, memberId: String, newRole: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = groupRepository.updateMemberRole(groupId, memberId, newRole)
            result.onSuccess {
                fetchMembers(groupId)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun removeMember(groupId: String, memberId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = groupRepository.removeMember(groupId, memberId)
            result.onSuccess {
                fetchMembers(groupId)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun renameGroup(groupId: String, newName: String) {
        if (newName.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = groupRepository.renameGroup(groupId, newName)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun leaveGroup(context: Context, groupId: String) {
        val user = authRepository.getCurrentUser() ?: return
        
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = groupRepository.leaveGroup(user.uuid, groupId)
            if (result.isSuccess) {
                // If there are no more groups, reset work mode
                val remainingGroups = _uiState.value.groups.filter { it.groupId != groupId }
                if (remainingGroups.isEmpty() || remainingGroups.all { it.name == "Home" }) {
                    SharedPreferencesManager.setWorkMode(context, 0)
                }
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
