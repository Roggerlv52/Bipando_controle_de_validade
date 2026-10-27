package com.rogger.bp.ui.home.view.componentes

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 27/10/2026
 * Hora: 18:33
 */
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rogger.bp.R

private val PromoBlack = Color(0xFF0A0A0A)
private val PromoYellow = Color(0xFFFFD60A)
private val PromoRed = Color(0xFFE5254B)

@Composable
fun TrialWelcomeDialog(
    days: Int,
    validUntil: String,          // ex.: "23/01/2027"
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(PromoBlack)
                .padding(24.dp)
        ) {
            // Selo no canto superior esquerdo
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PromoRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.trial_welcome_badge),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }

            // Botão fechar
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // "VOCÊ GANHOU" só com contorno
                OutlinedHeadline(
                    text = stringResource(R.string.trial_welcome_headline),
                    fontSize = 34.sp
                )

                Spacer(Modifier.height(20.dp))

                // Selo amarelo 1: quantidade de dias
                YellowBadge(
                    text = pluralStringResource(R.plurals.trial_welcome_days_badge, days, days),
                    fontSize = 48.sp,
                    rotation = -3f
                )

                Spacer(Modifier.height(10.dp))

                // Selo amarelo 2: ilimitado
                YellowBadge(
                    text = stringResource(R.string.trial_welcome_unlimited),
                    fontSize = 40.sp,
                    rotation = 2f
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.trial_welcome_subtitle),
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.trial_welcome_valid_until, validUntil),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PromoYellow,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = stringResource(R.string.trial_welcome_cta),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OutlinedHeadline(text: String, fontSize: TextUnit) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = Color.White,
            drawStyle = Stroke(width = 3f, join = StrokeJoin.Round)
        ),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun YellowBadge(text: String, fontSize: TextUnit, rotation: Float) {
    Box(
        modifier = Modifier
            .rotate(rotation)
            .clip(RoundedCornerShape(18.dp))
            .background(PromoYellow)
            .padding(horizontal = 28.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}