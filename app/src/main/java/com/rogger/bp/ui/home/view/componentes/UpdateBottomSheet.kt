package com.rogger.bp.ui.home.view.componentes

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.rogger.bp.R
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.theme.BipandoTheme

/*
 * Observações importantes sobre In-App Update:
 * - Esta implementação utiliza a API real da Google Play Store (AppUpdateManagerFactory).
 * - A API de In-App Update só funciona em aplicativos instalados e baixados via Google Play Store,
 *   quando existe uma versão publicada no Play Console com versionCode superior ao app atualmente instalado.
 * - Para realizar o teste real com a loja, instale uma versão via faixa de Teste Interno (Internal Test Track) do Play Console.
 */

// Estados do processo de atualização
sealed interface UpdateStatus {
    object Idle : UpdateStatus
    data class Downloading(val progress: Float) : UpdateStatus
    object Downloaded : UpdateStatus
}

@Composable
private fun rememberAppUpdateManager(): AppUpdateManager {
    val context = LocalContext.current
    return remember(context) {
        AppUpdateManagerFactory.create(context)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBottomSheet(
    appUpdateManager: AppUpdateManager = rememberAppUpdateManager()
) {
    val context = LocalContext.current

    var showSheet by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var dismissedInSession by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = {
            // Impede fechar por arrasto/toque fora enquanto o download estiver em andamento
            updateStatus !is UpdateStatus.Downloading
        }
    )

    val dismissAction = {
        showSheet = false
        dismissedInSession = true
        SharedPreferencesManager.setUpdateDismissedInSession(context, true)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK && updateStatus !is UpdateStatus.Downloading) {
            dismissAction()
        }
    }

    // Função de verificação do estado da atualização
    fun checkAppUpdate() {
        if (SharedPreferencesManager.isUpdateDismissedInSession(context)) {
            return
        }
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                updateStatus = UpdateStatus.Downloaded
                showSheet = true
            } else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                && !SharedPreferencesManager.isUpdateDismissedInSession(context)
            ) {
                updateInfo = info
                showSheet = true
            }
        }.addOnFailureListener {
            // Ignora falhas silenciosamente (aparelhos sem Play Store ou instalados fora da loja não devem causar crash)
        }
    }

    // Registra o listener do progresso/conclusão de download do fluxo FLEXIBLE
    DisposableEffect(appUpdateManager) {
        val listener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val total = state.totalBytesToDownload()
                    val downloaded = state.bytesDownloaded()
                    val progress = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    updateStatus = UpdateStatus.Downloading(progress)
                }
                InstallStatus.DOWNLOADED -> {
                    updateStatus = UpdateStatus.Downloaded
                }
                InstallStatus.FAILED, InstallStatus.CANCELED -> {
                    updateStatus = UpdateStatus.Idle
                }
                else -> {}
            }
        }
        appUpdateManager.registerListener(listener)
        onDispose {
            appUpdateManager.unregisterListener(listener)
        }
    }

    // Checagem inicial na composição
    LaunchedEffect(Unit) {
        checkAppUpdate()
    }

    // Verifica se a atualização foi baixada ao retornar ao app (ciclo de vida ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAppUpdate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ModalBottomSheet do Material 3 para aviso de nova versão disponível e progresso de download
    if (showSheet && !dismissedInSession) {
        ModalBottomSheet(
            onDismissRequest = {
                if (updateStatus !is UpdateStatus.Downloading) {
                    dismissAction()
                }
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            UpdateSheetContent(
                updateStatus = updateStatus,
                onUpdateClick = {
                    updateInfo?.let { info ->
                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                info,
                                launcher,
                                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                            )
                        } catch (_: Exception) {}
                    }
                },
                onDismissClick = {
                    dismissAction()
                },
                onRestartClick = {
                    appUpdateManager.completeUpdate()
                }
            )
        }
    }
}

/**
 * Conteúdo visual isolado do BottomSheet de Atualização
 */
@Composable
fun UpdateSheetContent(
    updateStatus: UpdateStatus,
    onUpdateClick: () -> Unit,
    onDismissClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (updateStatus as? UpdateStatus.Downloading)?.progress ?: 0f,
        animationSpec = tween(durationMillis = 300),
        label = "DownloadProgressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Ícone do App no topo
        Image(
            painter = painterResource(id = R.drawable.ic_bp_logo_small),
            contentDescription = stringResource(R.string.update_app_icon_description),
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Título e Mensagem centralizados
        val titleText = if (updateStatus is UpdateStatus.Downloaded) {
            stringResource(R.string.update_downloaded_title)
        } else {
            stringResource(R.string.update_title)
        }

        Text(
            text = titleText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (updateStatus !is UpdateStatus.Downloaded) {
            Text(
                text = stringResource(R.string.update_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Botões e Progresso empilhados verticalmente
        when (updateStatus) {
            is UpdateStatus.Idle -> {
                Button(
                    onClick = onUpdateClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.update_btn_now))
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismissClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.update_btn_later))
                }
            }

            is UpdateStatus.Downloading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.update_downloading),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (animatedProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(
                            R.string.update_progress_percent,
                            (animatedProgress * 100).toInt()
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is UpdateStatus.Downloaded -> {
                Button(
                    onClick = onRestartClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.update_btn_restart))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateSheetContentIdlePreview() {
    BipandoTheme {
        Surface {
            UpdateSheetContent(
                updateStatus = UpdateStatus.Idle,
                onUpdateClick = {},
                onDismissClick = {},
                onRestartClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateSheetContentDownloadingPreview() {
    BipandoTheme {
        Surface {
            UpdateSheetContent(
                updateStatus = UpdateStatus.Downloading(0.45f),
                onUpdateClick = {},
                onDismissClick = {},
                onRestartClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateSheetContentDownloadedPreview() {
    BipandoTheme {
        Surface {
            UpdateSheetContent(
                updateStatus = UpdateStatus.Downloaded,
                onUpdateClick = {},
                onDismissClick = {},
                onRestartClick = {}
            )
        }
    }
}
