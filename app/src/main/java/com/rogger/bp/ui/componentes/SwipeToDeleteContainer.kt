package com.rogger.bp.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SwipeToDeleteContainer(
    item: T,
    onDelete: ((T) -> Unit)? = null,
    onEdit: ((T) -> Unit)? = null,
    animationDuration: Int = 500,
    content: @Composable (T) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (onDelete != null) {
                        onDelete(item)
                    }
                    false // Retorna false para que o item não suma imediatamente, permitindo a confirmação externa
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (onEdit != null) {
                        onEdit(item)
                    }
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> if (onDelete != null) Color(0xFFD32F2F) else Color.Transparent
                SwipeToDismissBoxValue.StartToEnd -> if (onEdit != null) Color(0xFF1976D2) else Color.Transparent
                else -> Color.Transparent
            }

            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.Center
            }

            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> if (onDelete != null) Icons.Default.Delete else null
                SwipeToDismissBoxValue.StartToEnd -> if (onEdit != null) Icons.Default.Edit else null
                else -> null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                icon?.let {
                    Icon(
                        it,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        },
        enableDismissFromEndToStart = onDelete != null,
        enableDismissFromStartToEnd = onEdit != null,
        content = { content(item) }
    )
}
