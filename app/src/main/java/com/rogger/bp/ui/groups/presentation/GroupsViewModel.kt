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

data class GroupMember(
    val id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val role: String = "Reader" // "Admin", "Editor", "Reader"
)

data class GroupsState(
    val hasGroup: Boolean = false,
    val groupId: String = "",
    val groupName: String = "",
    val groupCode: String = "",
    val members: List<GroupMember> = emptyList(),
    val invitations: List<PostInvitation> = emptyList(),
    val isLoading: Boolean = false,
    val userPhoto: String = "",
    val userName: String = "",
    val userRole: String = "Admin",
    val showCreateDialog: Boolean = false,
    val showJoinDialog: Boolean = false,
    val error: String? = null
)

class GroupsViewModel(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsState())
    val uiState: StateFlow<GroupsState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeGroup()
        observeInvitations()
    }

    private fun observeGroup() {
        val currentUser = authRepository.getCurrentUser()
        groupRepository.getLocalGroupFlow().onEach { group ->
            if (group != null) {
                val isCollaborative = group.adminId != currentUser?.uuid || (group.name != "Meu Grupo" && group.name.isNotEmpty())
                _uiState.update { 
                    it.copy(
                        hasGroup = isCollaborative,
                        groupId = group.groupId,
                        groupName = group.name,
                        groupCode = group.shareCode
                    )
                }
                fetchMembers(group.groupId)
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
                    state.copy(
                        userRole = myRole,
                        members = members.map { 
                            GroupMember(it.userId, it.name, it.photoUrl, it.role)
                        }
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
            // Apenas sincroniza o que já existe. Não cria grupo novo aqui.
            groupRepository.syncUserGroup(userId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun createGroup(context: Context, name: String) {
        android.util.Log.d("GroupsViewModel", "createGroup called with name: $name")
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
                SharedPreferencesManager.setWorkMode(context, 1) // Ativa modo colaborativo
                _uiState.update { it.copy(showCreateDialog = false, hasGroup = true) }
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
            result.onSuccess {
                SharedPreferencesManager.setWorkMode(context, 1) // Ativa modo colaborativo
                _uiState.update { it.copy(showJoinDialog = false, hasGroup = true) }
            }
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun sendInvitation(targetCode: String, role: String) {
        if (targetCode.isBlank()) return
        val user = authRepository.getCurrentUser() ?: return
        val currentState = _uiState.value
        
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            // Busca o UID do usuário alvo através do código de convite (shareCode) dele
            val result = groupRepository.findGroupByCode(targetCode.uppercase())
            
            result.onSuccess { targetGroup ->
                val invitation = PostInvitation(
                    senderId = user.uuid,
                    senderName = user.name,
                    senderPhoto = user.photoUri?.toString() ?: "",
                    groupId = currentState.groupId,
                    groupName = currentState.groupName,
                    targetUid = targetGroup.adminId, // O adminId do grupo alvo é o UID do usuário
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

    fun cancelInvitation(invitation: PostInvitation) {
        viewModelScope.launch {
            groupRepository.cancelInvitation(invitation)
        }
    }

    fun toggleCreateDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateDialog = show) }
    }

    fun inviteUserByEmail(email: String, role: String) {
        if (email.isBlank()) return
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            // 1. Buscar usuário pelo e-mail
            val result = groupRepository.findUserByEmail(email)
            
            result.onSuccess { member ->
                if (member != null) {
                    // 2. Adicionar ao grupo atual
                    val group = groupRepository.getLocalGroupFlow().first()
                    if (group != null) {
                        val newMember = member.copy(role = role)
                        val addResult = groupRepository.addMemberToGroup(group.groupId, newMember)
                        
                        addResult.onSuccess {
                            fetchMembers(group.groupId) // Atualiza lista
                            _uiState.update { it.copy(error = null) }
                        }.onFailure { e ->
                            _uiState.update { it.copy(error = e.message) }
                        }
                    } else {
                        _uiState.update { it.copy(error = "Crie um grupo primeiro.") }
                    }
                } else {
                    _uiState.update { it.copy(error = "Nenhum usuário encontrado com este e-mail.") }
                }
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updateMemberRole(memberId: String, newRole: String) {
        val currentState = _uiState.value
        if (currentState.userRole != "Admin") return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val group = groupRepository.getLocalGroupFlow().first()
            if (group != null) {
                val result = groupRepository.updateMemberRole(group.groupId, memberId, newRole)
                result.onSuccess {
                    fetchMembers(group.groupId)
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun removeMember(memberId: String) {
        val currentState = _uiState.value
        if (currentState.userRole != "Admin") return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val group = groupRepository.getLocalGroupFlow().first()
            if (group != null) {
                val result = groupRepository.removeMember(group.groupId, memberId)
                result.onSuccess {
                    fetchMembers(group.groupId)
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun renameGroup(newName: String) {
        val currentState = _uiState.value
        if (currentState.userRole != "Admin") return
        if (newName.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val group = groupRepository.getLocalGroupFlow().first()
            if (group != null) {
                val result = groupRepository.renameGroup(group.groupId, newName)
                result.onSuccess {
                    // Local Group is updated via flow
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
