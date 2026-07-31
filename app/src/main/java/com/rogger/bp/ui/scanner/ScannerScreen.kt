package com.rogger.bp.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.DefaultDecoderFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear Produto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { context ->
                    CompoundBarcodeView(context).apply {
                        val formats = listOf(BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.QR_CODE)
                        decoderFactory = DefaultDecoderFactory(formats)
                        decodeContinuous { result ->
                            result.text?.let { 
                                pause() // Pausa para evitar múltiplas leituras
                                onBarcodeScanned(it) 
                            }
                        }
                        resume()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
