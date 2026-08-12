package com.rogger.bp.ui.profile.view

import android.app.TimePickerDialog
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rogger.bp.R
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.notification.NotificationUtil
import com.rogger.bp.ui.profile.presentation.ProfileState
import com.rogger.bp.ui.profile.presentation.ProfileViewModel
import com.rogger.bp.ui.theme.BipandoThemeType
import com.rogger.bp.util.ShareUtil
import com.rogger.bp.util.SystemSoundPickerDialog
import com.rogger.bp.util.NotificationSoundDialog
import com.rogger.bp.util.CreateGroupDialog
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onTrashClick: () -> Unit,
    onPaymentClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onNotificationToggle(context, true)
        }
    }

    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var showSystemSoundPicker by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showRemoveGroupDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current


    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Excluir Conta permanentemente") },
            text = {
                Column {
                    Text(
                        text = "ATENÇÃO: Esta ação é irreversível.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ao excluir sua conta, todos os seus dados serão apagados definitivamente do nosso sistema:")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Todos os seus produtos e categorias.")
                    Text("• Todas as imagens de produtos enviadas por você.")
                    Text("• Seu perfil e configurações de grupo.")
                    Text("• Status de assinatura Premium.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Deseja realmente prosseguir com a exclusão total dos seus dados?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(context)
                        showDeleteAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir Tudo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showSoundDialog) {
        NotificationSoundDialog(
            onDismiss = { showSoundDialog = false },
            onOptionSelected = { type, uri, name ->
                if (type == 3) {
                    showSystemSoundPicker = true
                } else {
                    viewModel.onSoundTypeChange(context, type, uri, name)
                }
            },
            currentType = state.soundType,
            currentUri = state.soundUri
        )
    }

    if (showSystemSoundPicker) {
        SystemSoundPickerDialog(
            context = context,
            onDismiss = { showSystemSoundPicker = false },
            onSoundSelected = { uri, name ->
                viewModel.onSoundTypeChange(context, 3, uri, name)
                showSoundDialog = false
            }
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name ->
                viewModel.createGroup(name)
                showCreateGroupDialog = false
            },
            isLoading = state.isLoading,
            error = state.errorMessage
        )
    }

    if (showRemoveGroupDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveGroupDialog = false },
            title = { Text("Remover Grupo") },
            text = { Text("Tem certeza que deseja sair deste grupo ou remover sua configuração colaborativa? Você voltará para o modo individual.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.leaveGroup()
                        showRemoveGroupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveGroupDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (state.syncSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSyncFlag() },
            title = { Text("Sincronização Concluída") },
            text = { Text("Seus produtos e categorias foram copiados para o grupo com sucesso!") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetSyncFlag() }) {
                    Text("OK")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile(context)
    }

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            onLogoutSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header Perfil
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = state.userPhotoUrl.ifEmpty { R.drawable.ic_person_24 },
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = state.userName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = state.userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Sempre mostra o código pessoal do usuário (baseado no UID)
                val personalCode = remember(state.userUid) { 
                    if (state.userUid.isNotEmpty()) state.userUid.take(8).uppercase() else "" 
                }

                if (personalCode.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        onClick = {
                            clipboardManager.setText(AnnotatedString(personalCode))
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Seu Código: $personalCode",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { ShareUtil.shareUserCode(context, personalCode) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartilhar Código",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            if (!state.hasCustomGroupName) {
                Button(
                    onClick = { showCreateGroupDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Criar Grupo Colaborativo")
                }
            } else {
                SettingsSection(title = "Meu Grupo") {
                    ListItem(
                        headlineContent = { Text(state.groupName, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Colaborativo") },
                        leadingContent = { Icon(Icons.Default.Group, contentDescription = null) },
                        trailingContent = { 
                            IconButton(onClick = { showRemoveGroupDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sair do Grupo", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("Exibir produtos de:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    
                    var showSyncDialog by remember { mutableStateOf(false) }

                    ThemeOption(
                        title = "Meus Produtos (Individual)",
                        selected = state.workMode == 0,
                        onClick = { viewModel.onWorkModeChange(context, 0) }
                    )
                    ThemeOption(
                        title = "Grupo (${state.groupName})",
                        selected = state.workMode == 1,
                        onClick = { 
                            if (state.workMode != 1) {
                                showSyncDialog = true
                            }
                        }
                    )

                    if (showSyncDialog) {
                        AlertDialog(
                            onDismissRequest = { 
                                showSyncDialog = false
                                viewModel.onWorkModeChange(context, 1)
                            },
                            title = { Text("Sincronizar Produtos?") },
                            text = { Text("Deseja copiar seus produtos e categorias individuais para o grupo colaborativo agora? Isso permitirá que outros membros vejam seus itens atuais.") },
                            confirmButton = {
                                Button(onClick = {
                                    viewModel.syncProductsToGroup()
                                    viewModel.onWorkModeChange(context, 1)
                                    showSyncDialog = false
                                }) {
                                    Text("Sincronizar e Ativar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    viewModel.onWorkModeChange(context, 1)
                                    showSyncDialog = false
                                }) {
                                    Text("Apenas Ativar")
                                }
                            }
                        )
                    }
                    
                    if (state.syncSuccess) {
                        LaunchedEffect(Unit) {
                            delay(3000)
                            viewModel.resetSyncFlag()
                        }
                    }
                }
            }

            PremiumStatusSection(state = state, onPaymentClick = onPaymentClick)

            SettingsSection(title = "Preferências") {
                Text(
                    text = "Tipo de Seletor de Data",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ThemeOption(
                    title = "Calendário (Padrão)",
                    selected = state.datePickerType == 0,
                    onClick = { viewModel.onDatePickerTypeChange(context, 0) }
                )
                ThemeOption(
                    title = "Spinner (Rolagem)",
                    selected = state.datePickerType == 1,
                    onClick = { viewModel.onDatePickerTypeChange(context, 1) }
                )
            }

            SettingsSection(title = "Aparência") {
                ThemeOption(
                    title = "Tema Clássico",
                    selected = state.themeType == BipandoThemeType.CLASSIC,
                    onClick = { viewModel.onThemeChange(context, BipandoThemeType.CLASSIC) }
                )
                ThemeOption(
                    title = "Tema Verde",
                    selected = state.themeType == BipandoThemeType.GREEN,
                    isPremiumOnly = true,
                    isUserPremium = state.isPremium,
                    onClick = { 
                        if (state.isPremium) {
                            viewModel.onThemeChange(context, BipandoThemeType.GREEN)
                        } else {
                            onPaymentClick()
                        }
                    }
                )
                ThemeOption(
                    title = "Tema Vermelho",
                    selected = state.themeType == BipandoThemeType.RED,
                    isPremiumOnly = true,
                    isUserPremium = state.isPremium,
                    onClick = { 
                        if (state.isPremium) {
                            viewModel.onThemeChange(context, BipandoThemeType.RED)
                        } else {
                            onPaymentClick()
                        }
                    }
                )
                ThemeOption(
                    title = "Tema Noturno",
                    selected = state.themeType == BipandoThemeType.DARK,
                    onClick = { viewModel.onThemeChange(context, BipandoThemeType.DARK) }
                )
            }

            SettingsSection(title = "Notificações") {
                SwitchSetting(
                    title = "Ativar Notificações",
                    icon = Icons.Default.Notifications,
                    checked = state.isNotificationEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (NotificationUtil.temPermissao(context)) {
                                viewModel.onNotificationToggle(context, true)
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        } else {
                            viewModel.onNotificationToggle(context, false)
                        }
                    }
                )

                if (state.isNotificationEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aviso de vencimento: ${state.notificationDays} dias antes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = state.notificationDays.toFloat(),
                        onValueChange = { viewModel.onNotificationDaysChange(context, it.toInt()) },
                        valueRange = 1f..30f,
                        steps = 29
                    )
                    
                    ListItem(
                        headlineContent = { Text("Horário da Notificação") },
                        trailingContent = { Text(state.notificationTime, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.clickable {
                            val hour = NotificationPrefs.getHour(context)
                            val minute = NotificationPrefs.getMinute(context)
                            TimePickerDialog(context, { _, h, m ->
                                viewModel.onNotificationTimeChange(context, h, m)
                            }, hour, minute, true).show()
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Som da Notificação") },
                        supportingContent = { Text(state.soundName) },
                        leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        modifier = Modifier.clickable { showSoundDialog = true }
                    )
                }
            }

            SettingsSection(title = "Sons") {
                SwitchSetting(
                    title = "Ativar Bip",
                    icon = Icons.Default.VolumeUp,
                    checked = state.isBeepEnabled,
                    onCheckedChange = { viewModel.onBeepToggle(context, it) }
                )
            }

            SettingsSection(title = "Gerenciamento") {
                ListItem(
                    headlineContent = { Text("Lixeira") },
                    supportingContent = { Text("Ver itens removidos") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                    trailingContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.deletedProductsCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ) {
                                    Text(state.deletedProductsCount.toString())
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable(onClick = onTrashClick),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showDeleteAccountDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Excluir Conta")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Política de Privacidade",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://roggerlv52.github.io/bipando/"))
                        context.startActivity(intent)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun PremiumStatusSection(state: ProfileState, onPaymentClick: () -> Unit) {
    SettingsSection(title = "Uso do Plano") {
        if (state.isPremium) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Assinante Premium",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Você possui cadastro ilimitado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Produtos Cadastrados",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${state.totalProductsCount}/100",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.totalProductsCount >= 100) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { (state.totalProductsCount.toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (state.totalProductsCount >= 100) Color.Red else Color(0xFF8BC34A),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                if (state.totalProductsCount >= 90) {
                    Text(
                        text = if (state.totalProductsCount >= 100) 
                            "Limite atingido! Torne-se Premium para continuar." 
                            else "Você está atingindo o limite gratuito.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onPaymentClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remover Limites")
                }
            }
        }
    }
}

@Composable
fun ThemeOption(
    title: String,
    selected: Boolean,
    isPremiumOnly: Boolean = false,
    isUserPremium: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (isPremiumOnly && !isUserPremium) 0.6f else 1f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = if (isPremiumOnly) isUserPremium else true
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.weight(1f)
        )
        if (isPremiumOnly && !isUserPremium) {
            Surface(
                color = Color(0xFFFFD700).copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "PREMIUM",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB8860B),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SwitchSetting(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
