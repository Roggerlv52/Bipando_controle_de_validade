package com.rogger.bp.ui.scanner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import com.rogger.bp.R
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.BarcodeView

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
    var barcodeView by remember { mutableStateOf<CompoundBarcodeView?>(null) }

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
                            onBarcodeScanned(manualBarcode.trim())
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
                                    onBarcodeScanned(it) 
                                }
                            }
                            //barcodeView = this
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
                    }

                    // Scanner Frame (Center)
                    Box(
                        modifier = Modifier
                            .width(frameWidth)
                            .height(frameHeight)
                            .align(Alignment.Center)
                    ) {
                        // Cantos do Scanner
                        val cornerSize = 40.dp
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
                                .padding(bottom = 10.dp, end = 10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = stringResource(R.string.scanner_bipando),
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Light,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = stringResource(R.string.scanner_subtitle),
                                color = Color(0xFF76FF03), // Verde/Lima da imagem
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Barra Inferior (Preta) com "Digitar codigo de barras"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Transparent.copy(alpha = 0.5f))
                            .navigationBarsPadding()
                            .clickable { showManualEntryDialog = true }
                            .padding(vertical = 24.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = stringResource(R.string.digitaliz_c_digo_de_barras),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
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
