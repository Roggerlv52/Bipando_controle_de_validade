package com.rogger.bp.ui.home.view.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rogger.bp.R

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 26/08/2026
 * Hora: 19:48
 */
@Composable
fun ProductGroupHeader(
    daysRemaining: Long,
    isGroupRemoving : Boolean,
    yellowWarningLimit: Int,
    userRole: String = "Admin",
    onRemoveGroup: (String) -> Unit
) {
    val isDark =
        MaterialTheme.colorScheme.surface.run { (red * 0.299 + green * 0.587 + blue * 0.114) < 0.5 }

    val statusText = when {
        daysRemaining < 1 -> {
            if (daysRemaining == 0L) stringResource(R.string.status_today)
            else stringResource(R.string.group_expired)
        }
        daysRemaining <= yellowWarningLimit -> {
            if (daysRemaining == 1L) stringResource(R.string.group_tomorrow)
            else stringResource(R.string.group_days_left, daysRemaining.toInt())
        }
        else -> stringResource(R.string.group_days_left, daysRemaining.toInt())
    }

    val (backgroundColor, dotColor, textColor) = remember(
        daysRemaining,
        yellowWarningLimit,
        isDark
    ) {
        val bg: Color
        val dot: Color
        val txt: Color

        when {
            daysRemaining < 1 -> {
                bg = if (isDark) Color(0xFF450A0A) else Color(0xFFFDF2F2)
                dot = Color(0xFFEF4444)
                txt = if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B)
            }

            daysRemaining <= yellowWarningLimit -> {
                bg = if (isDark) Color(0xFF451A03) else Color(0xFFFFFBEB)
                dot = Color(0xFFF59E0B)
                txt = if (isDark) Color(0xFFFED7AA) else Color(0xFF92400E)
            }

            else -> {
                bg = if (isDark) Color(0xFF064E3B) else Color(0xFFF0FDF4)
                dot = Color(0xFF10B981)
                txt = if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46)
            }
        }
        Triple(bg, dot, txt)
    }
    AnimatedVisibility(
        visible = !isGroupRemoving,
        exit = shrinkVertically(
            animationSpec = tween(600),
            shrinkTowards = Alignment.Top,
        ) + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = statusText,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            IconButton(
                onClick = { 
                    onRemoveGroup(statusText) 
                },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_remove_group),
                    tint = if (userRole.equals("Reader", ignoreCase = true)) 
                        textColor.copy(alpha = 0.2f) else textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
