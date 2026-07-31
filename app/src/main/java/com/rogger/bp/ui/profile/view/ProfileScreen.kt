package com.rogger.bp.ui.profile.view

import android.app.TimePickerDialog
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rogger.bp.R
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.ui.profile.presentation.ProfileViewModel
import com.rogger.bp.ui.theme.BipandoThemeType

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = state.userPhotoUrl.ifEmpty { R.drawable.ic_person_24 },
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = state.userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Gerencie suas preferências",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsSection(title = "Aparência") {
                ThemeOption(
                    title = "Tema Clássico (Escuro)",
                    selected = state.themeType == BipandoThemeType.CLASSIC,
                    onClick = { viewModel.onThemeChange(context, BipandoThemeType.CLASSIC) }
                )
                ThemeOption(
                    title = "Tema Verde",
                    selected = state.themeType == BipandoThemeType.GREEN,
                    onClick = { viewModel.onThemeChange(context, BipandoThemeType.GREEN) }
                )
                ThemeOption(
                    title = "Tema Vermelho",
                    selected = state.themeType == BipandoThemeType.RED,
                    onClick = { viewModel.onThemeChange(context, BipandoThemeType.RED) }
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
                    onCheckedChange = { viewModel.onNotificationToggle(context, it) }
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
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onTrashClick),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                ListItem(
                    headlineContent = { Text("Assinatura Premium") },
                    supportingContent = { Text("Gerencie seu plano") },
                    leadingContent = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D)) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onPaymentClick),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.logout(context) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sair da Conta")
            }

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
fun ThemeOption(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
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
