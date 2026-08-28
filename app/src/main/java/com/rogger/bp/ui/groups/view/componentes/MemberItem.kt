package com.rogger.bp.ui.groups.view.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rogger.bp.ui.groups.presentation.GroupMember

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 26/08/2026
 * Hora: 20:31
 */
@Composable
fun MemberItem(
    member: GroupMember,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(member.name, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(if (member.role == "Admin")
            "Administrador" else if (member.role == "Editor")
                "Editor" else "Leitor") },
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            AsyncImage(
                model = member.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            AssistChip(
                onClick = onClick,
                label = { Text(member.role, fontSize = 10.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = if (member.role == "Admin") MaterialTheme.colorScheme.primary else Color.Gray
                )
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
