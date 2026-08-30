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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rogger.bp.R
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.notification.NotificationUtil
import com.rogger.bp.ui.profile.presentation.ProfileState
import com.rogger.bp.ui.profile.presentation.ProfileViewModel
import com.rogger.bp.ui.theme.BipandoThemeType
import com.rogger.bp.util.ShareUtil
import com.rogger.bp.util.SystemSoundPickerDialog
import com.rogger.bp.util.NotificationSoundDialog
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration

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

    val clipboardManager = LocalClipboardManager.current


    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(stringResource(R.string.profile_delete_account_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.profile_delete_warning_header),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.profile_delete_warning_body))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.profile_delete_item_1))
                    Text(stringResource(R.string.profile_delete_item_2))
                    Text(stringResource(R.string.profile_delete_item_3))
                    Text(stringResource(R.string.profile_delete_item_4))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.profile_delete_final_question))
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
                    Text(stringResource(R.string.profile_delete_all_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(stringResource(R.string.dialog_button_cancel))
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
            currentUri = state.soundUri,
            currentName = state.soundName
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
                title = { Text(stringResource(R.string.profile_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.userPhotoUrl.ifEmpty { R.drawable.ic_person_24 })
                        .size(300, 300) // Otimizado para o perfil de 100dp
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.cd_profile_photo),
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
                            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.cd_copy), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profile_user_code, personalCode),
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
                                    contentDescription = stringResource(R.string.cd_share_code),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            PremiumStatusSection(state = state, onPaymentClick = onPaymentClick)

            SettingsSection(title = stringResource(R.string.profile_preferences)) {
                Text(
                    text = stringResource(R.string.profile_date_picker_type),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ThemeOption(
                    title = stringResource(R.string.profile_date_picker_calendar),
                    selected = state.datePickerType == 0,
                    onClick = { viewModel.onDatePickerTypeChange(context, 0) }
                )
                ThemeOption(
                    title = stringResource(R.string.profile_date_picker_spinner),
                    selected = state.datePickerType == 1,
                    onClick = { viewModel.onDatePickerTypeChange(context, 1) }
                )
            }

            SettingsSection(title = stringResource(R.string.profile_appearance)) {
                ThemeOption(
                    title = stringResource(R.string.profile_theme_classic),
                    selected = state.themeType == BipandoThemeType.CLASSIC,
                    onClick = { viewModel.onThemeChange(context, BipandoThemeType.CLASSIC) }
                )
                ThemeOption(
                    title = stringResource(R.string.profile_theme_green),
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
                    title = stringResource(R.string.profile_theme_red),
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
                    title = stringResource(R.string.profile_theme_dark),
                    selected = state.themeType == BipandoThemeType.DARK,
                    onClick = { viewModel.onThemeChange(context, BipandoThemeType.DARK) }
                )
            }

            SettingsSection(title = stringResource(R.string.profile_notifications)) {
                SwitchSetting(
                    title = stringResource(R.string.profile_enable_notifications),
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
                        text = stringResource(R.string.profile_expiry_warning_label, state.notificationDays),
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
                        headlineContent = { Text(stringResource(R.string.profile_notification_time)) },
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
                        headlineContent = { Text(stringResource(R.string.profile_notification_sound)) },
                        supportingContent = { Text(state.soundName) },
                        leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        modifier = Modifier.clickable { showSoundDialog = true }
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.profile_sounds)) {
                SwitchSetting(
                    title = stringResource(R.string.profile_enable_beep),
                    icon = Icons.Default.VolumeUp,
                    checked = state.isBeepEnabled,
                    onCheckedChange = { viewModel.onBeepToggle(context, it) }
                )
            }

            SettingsSection(title = stringResource(R.string.profile_management)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.profile_trash)) },
                    supportingContent = { Text(stringResource(R.string.profile_view_deleted)) },
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
                Text(stringResource(R.string.profile_delete_account_button))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.profile_privacy_policy),
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
    SettingsSection(title = stringResource(R.string.profile_plan_usage)) {
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
                        text = stringResource(R.string.profile_premium_subscriber),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.profile_unlimited_registration),
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
                        text = stringResource(R.string.profile_registered_products),
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
                            stringResource(R.string.profile_limit_reached_msg) 
                            else stringResource(R.string.profile_approaching_limit_msg),
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
                    Text(stringResource(R.string.profile_remove_limits))
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
