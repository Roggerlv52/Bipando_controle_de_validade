package com.rogger.bp.ui.groups.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rogger.bp.ui.componentes.BipandoTextField
import com.rogger.bp.ui.groups.presentation.GroupsViewModel
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
    var groupCodeInput by remember { mutableStateOf("") }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showEditGroupNameDialog by remember { mutableStateOf(false) }
    var memberToManage by remember { mutableStateOf<com.rogger.bp.ui.groups.presentation.GroupMember?>(null) }

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
            onAccept = { viewModel.respondInvitation(invitation, true) },
            onDecline = { viewModel.respondInvitation(invitation, false) }
        )
    }

    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddUserDialog = false 
                viewModel.clearError()
            },
            title = { Text("Convidar Membro") },
            text = {
                Column {
                    Text("O usuário convidado poderá colaborar na sua lista de produtos.")
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
                        RadioButton(selected = selectedRole == "Editor", onClick = { selectedRole = "Editor" })
                        Text("Editor")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = selectedRole == "Reader", onClick = { selectedRole = "Reader" })
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
                        viewModel.sendInvitation(newUserCode, selectedRole)
                        // A verificação de fechar o dialog pode ser feita via LaunchedEffect observando o estado
                    },
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Convidar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddUserDialog = false 
                    viewModel.clearError()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showEditGroupNameDialog) {
        var newName by remember { mutableStateOf(state.groupName) }
        AlertDialog(
            onDismissRequest = { showEditGroupNameDialog = false },
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
                    viewModel.renameGroup(newName)
                    showEditGroupNameDialog = false 
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGroupNameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    memberToManage?.let { member ->
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
                                viewModel.updateMemberRole(member.id, if (member.role == "Editor") "Reader" else "Editor")
                                memberToManage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tornar ${if (member.role == "Editor") "Leitor" else "Editor"}")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { 
                                viewModel.removeMember(member.id)
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

    // Fecha o dialog se o convite for enviado com sucesso
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && state.error == null && newUserCode.isNotEmpty()) {
            showAddUserDialog = false
            newUserCode = ""
        }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Criar ou Entrar em um Grupo") },
            text = {
                Column {
                    Text("Escolha uma opção para começar a colaborar.")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BipandoTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = "Nome do novo grupo",
                        leadingIcon = Icons.Default.Add
                    )
                    Button(
                        onClick = { viewModel.createGroup(context, groupNameInput) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Criar Novo Grupo")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    BipandoTextField(
                        value = groupCodeInput,
                        onValueChange = { groupCodeInput = it },
                        label = "Código de convite",
                        leadingIcon = Icons.Default.Check
                    )
                    Button(
                        onClick = { viewModel.joinGroup(context, groupCodeInput) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Entrar em Grupo Existente")
                    }

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
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LaunchedEffect(state.hasGroup) {
        if (state.hasGroup) {
            showCreateGroupDialog = false
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grupo de Usuários", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (state.hasGroup) {
                        IconButton(onClick = { showAddUserDialog = true }) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Adicionar Usuário")
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLoading && !state.hasGroup) {
                // Carregamento inicial: centraliza um loader
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.hasGroup) {
                //--------------------------------------------------------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp), // Borda reta para encostar nas laterais
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.groupName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            if (state.userRole == "Admin") {
                                IconButton(onClick = { showEditGroupNameDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Renomear", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Text(
                            text = "Seu código de convite: ${state.groupCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(state.groupCode))
                            }
                        )
                    }
                }

                // Conteúdo abaixo do card com a margem original de 24.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.invitations.isNotEmpty()) {
                        Text(
                            text = "Convites Pendentes",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        state.invitations.forEach { invitation ->
                            InvitationItem(
                                invitation = invitation,
                                onAccept = { viewModel.respondInvitation(invitation, true) },
                                onDecline = { viewModel.respondInvitation(invitation, false) }
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Text(
                        text = "Membros do Grupo",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.members.isEmpty()) {
                            item {
                                Text(
                                    "Nenhum membro adicionado além de você.",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                                )
                            }
                        }
                        items(state.members) { member ->
                            MemberItem(
                                member = member,
                                onClick = {
                                    if (state.userRole == "Admin") {
                                        memberToManage = member
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Estado vazio: sem grupo - mantendo padding de 24.dp
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.invitations.isNotEmpty()) {
                        Text(
                            text = "Convites Pendentes",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        state.invitations.forEach { invitation ->
                            InvitationItem(
                                invitation = invitation,
                                onAccept = { viewModel.respondInvitation(invitation, true) },
                                onDecline = { viewModel.respondInvitation(invitation, false) }
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.LightGray
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
}

@Composable
fun InvitationItem(
    invitation: com.rogger.bp.data.model.PostInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        ListItem(
            headlineContent = { Text("${invitation.senderName} convidou você", fontWeight = FontWeight.Bold) },
            supportingContent = { Text("Grupo: ${invitation.groupName}") },
            leadingContent = {
                AsyncImage(
                    model = invitation.senderPhoto,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
            },
            trailingContent = {
                Row {
                    IconButton(onClick = onAccept) {
                        Icon(Icons.Default.Check, contentDescription = "Aceitar", tint = Color(0xFF4CAF50))
                    }
                    IconButton(onClick = onDecline) {
                        Icon(Icons.Default.Close, contentDescription = "Recusar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun MemberItem(
    member: com.rogger.bp.ui.groups.presentation.GroupMember,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(member.name, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(if (member.role == "Admin") "Administrador" else if (member.role == "Editor") "Editor" else "Leitor") },
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            AsyncImage(
                model = member.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            AssistChip(
                onClick = onClick,
                label = { Text(member.role) },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = if (member.role == "Admin") MaterialTheme.colorScheme.primary else Color.Gray
                )
            )
        }
    )
}
