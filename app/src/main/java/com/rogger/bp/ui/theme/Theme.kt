package com.rogger.bp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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

    // No Android 15+, enableEdgeToEdge() na Activity já cuida de tudo.
    // Removido SideEffect que usava APIs descontinuadas.

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
