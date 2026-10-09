package com.rogger.bp.util

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rogger.bp.R
import java.util.*

private sealed interface VoiceStatus {
    object HoldToSpeak : VoiceStatus
    object Listening : VoiceStatus
    object NotAvailable : VoiceStatus
    object ErrorNoMatch : VoiceStatus
    object ErrorNetwork : VoiceStatus
    object ErrorPermissions : VoiceStatus
    data class ErrorGeneric(val code: Int) : VoiceStatus
}

@Composable
fun VoiceSearchDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    
    val isAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    var statusState by remember {
        mutableStateOf<VoiceStatus>(
            if (isAvailable) VoiceStatus.HoldToSpeak else VoiceStatus.NotAvailable
        )
    }
    
    val speechRecognizer = remember { 
        if (isAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null 
    }

    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    DisposableEffect(isAvailable) {
        if (!isAvailable || speechRecognizer == null) return@DisposableEffect onDispose {}

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { 
                statusState = VoiceStatus.Listening
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { 
                isListening = false 
            }
            override fun onError(error: Int) {
                isListening = false
                statusState = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> VoiceStatus.ErrorNoMatch
                    SpeechRecognizer.ERROR_NETWORK -> VoiceStatus.ErrorNetwork
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceStatus.ErrorPermissions
                    else -> VoiceStatus.ErrorGeneric(error)
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                    onDismiss()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val statusText = when (val status = statusState) {
        VoiceStatus.HoldToSpeak -> stringResource(R.string.voice_search_hold_to_speak)
        VoiceStatus.Listening -> stringResource(R.string.voice_search_listening)
        VoiceStatus.NotAvailable -> stringResource(R.string.voice_search_not_available)
        VoiceStatus.ErrorNoMatch -> stringResource(R.string.voice_search_error_no_match)
        VoiceStatus.ErrorNetwork -> stringResource(R.string.voice_search_error_network)
        VoiceStatus.ErrorPermissions -> stringResource(R.string.voice_search_error_permissions)
        is VoiceStatus.ErrorGeneric -> stringResource(R.string.voice_search_error_generic, status.code)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.voice_search_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.voice_search_subtitle),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(pulseScale)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        )
                    }
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                            .pointerInput(isAvailable) {
                                if (!isAvailable) return@pointerInput
                                detectTapGestures(
                                    onPress = {
                                        try {
                                            isListening = true
                                            speechRecognizer?.startListening(recognizerIntent)
                                            tryAwaitRelease()
                                        } finally {
                                            isListening = false
                                            speechRecognizer?.stopListening()
                                        }
                                    }
                                )
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.cd_microphone),
                            modifier = Modifier.size(40.dp),
                            tint = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_button_cancel))
                }
            }
        }
    }
}
