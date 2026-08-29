package com.rogger.bp.ui.groups.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rogger.bp.data.model.PostGroup
import com.rogger.bp.ui.componentes.BipandoTextField
import com.rogger.bp.ui.componentes.SwipeToDeleteContainer
import com.rogger.bp.ui.groups.presentation.GroupMember
import com.rogger.bp.ui.groups.presentation.GroupsViewModel
import com.rogger.bp.ui.groups.view.componentes.GroupItem
import com.rogger.bp.ui.groups.view.componentes.InvitationItem
import com.rogger.bp.util.InvitationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var newUserCode by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Reader") }
    var groupNameInput by remember { mutableStateOf("") }

    var groupToInvite by remember { mutableStateOf<PostGroup?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<PostGroup?>(null) }
    var groupToLeave by remember { mutableStateOf<PostGroup?>(null) }

    var memberToManage by remember { mutableStateOf<Pair<String, GroupMember>?>(null) }
    var expandedGroupId by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(Unit) {
        viewModel.loadWorkMode(context)
    }

    // Dispara notificação local ao receber novos convites
    LaunchedEffect(state.invitations) {
        state.invitations.forEach { invitation ->
            com.rogger.bp.notification.NotificationUtil.showInvitation(
                context,
                invitation.senderName,
                invitation.groupName
            )
        }
    }

    // Mostra o primeiro convite pendente (se houver)
    state.invitations.firstOrNull()?.let { invitation ->
        InvitationDialog(
            invitation = invitation,
            onAccept = { viewModel.respondInvitation(context, invitation, true) },
            onDecline = { viewModel.respondInvitation(context, invitation, false) }
        )
    }

    groupToInvite?.let { group ->
        AlertDialog(
            onDismissRequest = {
                groupToInvite = null
                viewModel.clearError()
            },
            title = { Text("Convidar para ${group.name}") },
            text = {
                Column {
                    Text("O usuário convidado poderá colaborar na lista do grupo ${group.name}.")
                    Spacer(modifier = Modifier.height(16.dp))
                    BipandoTextField(
                        value = newUserCode,
                        onValueChange = { newUserCode = it },
                        label = "Código de 8 dígitos do Usuário",
                        leadingIcon = Icons.Default.Person
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Permissão:", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedRole == "Editor",
                            onClick = { selectedRole = "Editor" })
                        Text("Editor")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = selectedRole == "Reader",
                            onClick = { selectedRole = "Reader" })
                        Text("Leitor")
                    }
                    Text(
                        text = if (selectedRole == "Editor")
                            "Pode adicionar, editar e remover permanentemente."
                        else "Pode adicionar e editar (exceto remoção permanente).",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )

                    if (state.error != null) {
                        Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendInvitation(group.groupId, newUserCode, selectedRole)
                    },
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Convidar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    groupToInvite = null
                    viewModel.clearError()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    groupToEdit?.let { group ->
        var newName by remember { mutableStateOf(group.name) }
        AlertDialog(
            onDismissRequest = { groupToEdit = null },
            title = { Text("Renomear Grupo") },
            text = {
                BipandoTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = "Novo Nome do Grupo",
                    leadingIcon = Icons.Default.Edit
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameGroup(group.groupId, newName)
                    groupToEdit = null
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    groupToLeave?.let { group ->
        AlertDialog(
            onDismissRequest = { groupToLeave = null },
            title = { Text("Sair do Grupo") },
            text = { Text("Tem certeza que deseja sair do grupo \"${group.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.leaveGroup(context, group.groupId)
                        groupToLeave = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToLeave = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    memberToManage?.let { (groupId, member) ->
        AlertDialog(
            onDismissRequest = { memberToManage = null },
            title = { Text("Gerenciar Membro") },
            text = {
                Column {
                    Text("Membro: ${member.name}", fontWeight = FontWeight.Bold)
                    Text("Papel atual: ${if (member.role == "Admin") "Administrador" else if (member.role == "Editor") "Editor" else "Leitor"}")

                    Spacer(modifier = Modifier.height(16.dp))

                    if (member.role != "Admin") {
                        Button(
                            onClick = {
                                viewModel.updateMemberRole(
                                    groupId,
                                    member.id,
                                    if (member.role == "Editor") "Reader" else "Editor"
                                )
                                memberToManage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tornar ${if (member.role == "Editor") "Leitor" else "Editor"}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.removeMember(groupId, member.id)
                                memberToManage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Remover do Grupo")
                        }
                    } else {
                        Text("O Administrador não pode ter seu papel alterado.")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { memberToManage = null }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Fecha o dialog de convite se for enviado com sucesso
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && state.error == null && newUserCode.isNotEmpty()) {
            groupToInvite = null
            newUserCode = ""
        }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Criar Novo Grupo") },
            text = {
                Column {
                    Text("Dê um nome ao seu grupo para começar a colaborar com outros usuários.")
                    Spacer(modifier = Modifier.height(16.dp))

                    BipandoTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = "Nome do Grupo",
                        leadingIcon = Icons.Default.Groups
                    )

                    if (state.error != null) {
                        Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createGroup(context, groupNameInput) },
                    enabled = groupNameInput.isNotBlank() && !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Criar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LaunchedEffect(state.groups) {
        if (state.groups.any { it.name == groupNameInput }) {
            showCreateGroupDialog = false
            groupNameInput = ""
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grupos de Usuários", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateGroupDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Novo Grupo")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refreshGroups() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isLoading && state.groups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.groups.isNotEmpty() || state.invitations.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.groups, key = { it.groupId }) { group ->
                        val members = state.membersMap[group.groupId] ?: emptyList()
                        val admin = members.find { it.role == "Admin" }?.name ?: ""

                        SwipeToDeleteContainer(
                            item = group,
                            onDelete = if (!group.isDefault) { { groupToLeave = it } } else null,
                            onEdit = { groupToEdit = it }
                        ) {
                            GroupItem(
                                groupName = group.name,
                                isDefault = group.isDefault,
                                participantCount = members.size,
                                activeItemsCount = state.activeItemsCountMap[group.groupId]
                                    ?: 0,
                                members = members,
                                adminName = admin,
                                isExpanded = expandedGroupId == group.groupId,
                                isSelected = state.workMode == 1 && state.groupId == group.groupId,
                                onExpandClick = {
                                    expandedGroupId =
                                        if (expandedGroupId == group.groupId) null else group.groupId
                                },
                                onMemberClick = { member ->
                                    if (state.userRole == "Admin") {
                                        memberToManage = group.groupId to member
                                    }
                                },
                                onSelectGroup = {
                                    viewModel.onWorkModeChange(context, 1, group.groupId)
                                },
                                onInviteClick = { groupToInvite = group }
                            )
                        }
                    }
                    if (state.invitations.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Convites Pendentes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(state.invitations) { invitation ->
                            InvitationItem(
                                invitation = invitation,
                                onAccept = { viewModel.respondInvitation(context, invitation, true) },
                                onDecline = { viewModel.respondInvitation(context, invitation, false) }
                            )
                        }
                    }
                }
            } else {
                    // Estado vazio ou erro
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (state.error != null && state.groups.isEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Ocorreu um erro ao carregar seus grupos.\n\n${state.error}",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { viewModel.refreshGroups() }) {
                                Text("Tentar Novamente")
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Você ainda não participa de um grupo colaborativo.\n\nCrie o seu grupo ou peça para ser convidado.",
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { showCreateGroupDialog = true }) {
                                Text("Começar Agora")
                            }
                        }
                    }
                }
            }
        }
    }
}