package com.rogger.bp.util

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

data class SoundOption(
    val id: Int,
    val name: String,
    val icon: ImageVector,
    val uri: String = ""
)

@Composable
fun NotificationSoundDialog(
    onDismiss: () -> Unit,
    onOptionSelected: (Int, String, String) -> Unit,
    currentType: Int,
    currentUri: String
) {
    val options = listOf(
        SoundOption(0, "Mudo", Icons.Default.VolumeMute),
        SoundOption(1, "Vibrar", Icons.Default.Vibration),
        SoundOption(2, "Vibrar e Tocar (Padrão)", Icons.Default.NotificationsActive),
        SoundOption(3, "Escolher Som do Sistema", Icons.Default.MusicNote)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tipo de Alerta") },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (option.id == 3) {
                                    // Sinaliza que quer abrir a lista do sistema
                                    onOptionSelected(3, "", "")
                                } else {
                                    onOptionSelected(option.id, "", option.name)
                                    onDismiss()
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentType == option.id,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(option.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(option.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

fun getSystemRingtones(context: Context): List<Pair<String, String>> {
    val ringtoneManager = RingtoneManager(context)
    ringtoneManager.setType(RingtoneManager.TYPE_NOTIFICATION)
    val cursor = ringtoneManager.cursor
    val list = mutableListOf<Pair<String, String>>()
    while (cursor.moveToNext()) {
        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
        val uri = ringtoneManager.getRingtoneUri(cursor.position).toString()
        list.add(title to uri)
    }
    return list
}

@Composable
fun SystemSoundPickerDialog(
    context: Context,
    onDismiss: () -> Unit,
    onSoundSelected: (String, String) -> Unit
) {
    val ringtones = remember { getSystemRingtones(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sons de Notificação") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(ringtones) { (name, uri) ->
                    Text(
                        text = name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSoundSelected(uri, name)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Voltar")
            }
        }
    )
}
