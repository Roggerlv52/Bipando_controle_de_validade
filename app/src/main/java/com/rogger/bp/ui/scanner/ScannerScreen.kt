package com.rogger.bp.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.rogger.bp.R
import com.rogger.bp.ui.commun.AnalyticsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showManualEntryDialog by remember { mutableStateOf(false) }
    var barcodeView by remember { mutableStateOf<BarcodeView?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gerencia pausa/resumo quando o dialog abre/fecha
    LaunchedEffect(showManualEntryDialog) {
        if (showManualEntryDialog) {
            barcodeView?.pause()
        } else {
            // Só resume se tiver permissão e não estiver em processo de navegação
            if (hasCameraPermission) {
                barcodeView?.resume()
            }
        }
    }

    if (showManualEntryDialog) {
        var manualBarcode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManualEntryDialog = false },
            title = { Text(stringResource(R.string.scanner_manual_title)) },
            text = {
                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { manualBarcode = it },
                    label = { Text(stringResource(R.string.scanner_manual_hint)) },
                    supportingText = {
                        if (manualBarcode.isNotEmpty() && manualBarcode.length < 4) {
                            Text(stringResource(R.string.scanner_manual_error), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            },
            confirmButton = {
                val isValid = manualBarcode.trim().length >= 4
                Button(
                    onClick = {
                        if (isValid) {
                            val trimmed = manualBarcode.trim()
                            AnalyticsManager.logBarcodeScanned("manual")
                            onBarcodeScanned(trimmed)
                            showManualEntryDialog = false
                        }
                    },
                    enabled = isValid
                ) {
                    Text(stringResource(R.string.scanner_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualEntryDialog = false }) {
                    Text(stringResource(R.string.dialog_button_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        BarcodeView(ctx).apply {
                            val formats =
                                listOf(BarcodeFormat.EAN_13,
                                BarcodeFormat.EAN_8,
                                BarcodeFormat.CODE_128,
                                BarcodeFormat.QR_CODE)
                            decoderFactory = DefaultDecoderFactory(formats)
                            decodeContinuous { result ->
                                result.text?.let { 
                                    pause()
                                    AnalyticsManager.logBarcodeScanned("camera")
                                    onBarcodeScanned(it)
                                }
                            }
                            barcodeView = this
                            resume()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay Customizado conforme imagem de referência
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val isLandscape = maxWidth > maxHeight
                    val frameWidth = maxWidth * 0.85f
                    val frameHeight = if (isLandscape) maxHeight * 0.5f else frameWidth * 0.75f

                    // Barra Superior (Preta)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent.copy(alpha = 0.5f))
                            .statusBarsPadding()
                            .height(56.dp)
                            .align(Alignment.TopCenter),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.scanner_title),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    // Scanner Frame (Center)
                    Box(
                        modifier = Modifier
                            .width(frameWidth)
                            .height(frameHeight)
                            .align(Alignment.Center)
                    ) {
                        // Cantos do Scanner
                        val cornerSize = 30.dp
                        Image(
                            painter = painterResource(id = R.drawable.line_top_left),
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.TopStart).size(cornerSize)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.line_top_right),
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.TopEnd).size(cornerSize)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.line_down_left),
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomStart).size(cornerSize)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.line_down_right),
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomEnd).size(cornerSize)
                        )

                        // Nome do App (Bipando) - Estilo da imagem
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 1.dp, end = 30.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.bp_title),
                                contentDescription = stringResource(R.string.cd_logo_bipando),
                                modifier = Modifier.width(80.dp)
                            )
                        }
                    }

                    // Barra Inferior (Preta) com "Digitar codigo de barras"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomEnd)
                            .background(Color.Transparent.copy(alpha = 0.5f))
                            .navigationBarsPadding()
                            .padding(top = 16.dp,bottom = 5.dp, end = 16.dp),  // margem fixa bottom-end
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Surface(
                            modifier = Modifier
                                .clickable { showManualEntryDialog = true }
                                .wrapContentSize(),                  // garante que não colapse
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.3f), // cor como parâmetro correto
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                text = stringResource(R.string.digitaliz_c_digo_de_barras),
                                color = Color.Red.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1                         // evita quebra de linha em landscape
                            )
                        }
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> {
                                if (!showManualEntryDialog) barcodeView?.resume()
                            }
                            Lifecycle.Event.ON_PAUSE -> barcodeView?.pause()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        barcodeView?.pause()
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(R.string.scanner_permission_needed),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text(stringResource(R.string.scanner_grant_permission))
                    }
                }
            }
        }
    }
}
