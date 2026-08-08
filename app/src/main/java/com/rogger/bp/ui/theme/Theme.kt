package com.rogger.bp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.rogger.bp.ui.commun.SharedPreferencesManager

enum class BipandoThemeType {
    CLASSIC, GREEN, RED, DARK
}

private val ClassicColorScheme = lightColorScheme(
    primary = BipandoPrimary,
    secondary = BipandoSecondary,
    background = BipandoBackground,
    surface = White,
    error = ErrorColor
)

private val GreenColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = BipandoSecondary,
    background = BipandoBackground
)

private val RedColorScheme = lightColorScheme(
    primary = RedPrimary,
    secondary = BipandoSecondary,
    background = BipandoBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = BipandoPrimary,
    onPrimary = White,
    secondary = BipandoSecondary,
    onSecondary = Black,
    background = Black,
    onBackground = White,
    surface = GrayDarker,
    onSurface = White,
    surfaceVariant = Color(0xFF3B3B3B),
    onSurfaceVariant = Silver,
    outline = Silver
)

@Composable
fun BipandoTheme(
    themeType: BipandoThemeType? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val storedThemeNumber = remember { mutableStateOf(SharedPreferencesManager.getThemeNumber(context, "chave")) }

    val currentThemeType = themeType ?: when (storedThemeNumber.value) {
        1 -> BipandoThemeType.CLASSIC
        2 -> BipandoThemeType.GREEN
        3 -> BipandoThemeType.RED
        4 -> BipandoThemeType.DARK
        else -> BipandoThemeType.CLASSIC
    }

    val colorScheme = when {
        currentThemeType == BipandoThemeType.DARK -> DarkColorScheme
        currentThemeType == BipandoThemeType.GREEN -> GreenColorScheme
        currentThemeType == BipandoThemeType.RED -> RedColorScheme
        else -> ClassicColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Tornar transparente para permitir que a imagem fique sob as barras
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            // Garantir que a imagem ocupe o espaço das barras mesmo em rotação
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Forçar ícones da barra de status a serem sempre claros (branco)
            // pois o fundo da LoginScreen é sempre escuro/colorido
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
