package com.rogger.bp.ui.groups.view.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 26/08/2026
 * Hora: 20:30
 */
@Composable
fun InvitationItem(
    invitation: com.rogger.bp.data.model.PostInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                alpha = 0.3f
            )
        )
    ) {
        ListItem(
            headlineContent = {
                Text(
                    "${invitation.senderName} convidou você",
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = { Text("Grupo: ${invitation.groupName}") },
            leadingContent = {
                AsyncImage(
                    model = invitation.senderPhoto,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
            },
            trailingContent = {
                Row {
                    IconButton(onClick = onAccept) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Aceitar",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                    IconButton(onClick = (onDecline)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Recusar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}