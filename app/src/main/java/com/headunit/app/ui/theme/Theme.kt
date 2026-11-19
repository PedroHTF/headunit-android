package com.headunit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.headunit.app.settings.ThemeMode

/*
 * Paletas. Os mesmos valores estão no headunit-linux (qml/Theme.qml).
 * O alto contraste usa preto/branco puros e cores de destaque fortes (amarelo / azul escuro).
 */

private val Dark = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00222E),
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF0B0E13),
    onBackground = Color(0xFFE6E9EF),
    surface = Color(0xFF12161D),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF1B212A),
    onSurfaceVariant = Color(0xFF9AA3B2),
    outline = Color(0xFF3A4350),
    secondaryContainer = Color(0xFF253041),
    onSecondaryContainer = Color(0xFFE6E9EF),
)

private val Light = lightColorScheme(
    primary = Color(0xFF0277BD),
    onPrimary = Color.White,
    secondary = Color(0xFF00796B),
    background = Color(0xFFF3F5F8),
    onBackground = Color(0xFF12161D),
    surface = Color.White,
    onSurface = Color(0xFF12161D),
    surfaceVariant = Color(0xFFE6EAF0),
    onSurfaceVariant = Color(0xFF4A5260),
    outline = Color(0xFFB8C0CC),
    secondaryContainer = Color(0xFFCFE3F2),
    onSecondaryContainer = Color(0xFF12161D),
)

private val DarkHighContrast = darkColorScheme(
    primary = Color(0xFFFFD600),
    onPrimary = Color.Black,
    secondary = Color(0xFF00E5FF),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color.White,
    outline = Color.White,
    secondaryContainer = Color(0xFFFFD600),
    onSecondaryContainer = Color(0xFF000000),
)

private val LightHighContrast = lightColorScheme(
    primary = Color(0xFF0033CC),
    onPrimary = Color.White,
    secondary = Color(0xFF6A00A8),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
    secondaryContainer = Color(0xFF0033CC),
    onSecondaryContainer = Color(0xFFFFFFFF),
)

fun colorSchemeFor(dark: Boolean, highContrast: Boolean): ColorScheme = when {
    dark && highContrast -> DarkHighContrast
    dark -> Dark
    highContrast -> LightHighContrast
    else -> Light
}

/**
 * @param mode AUTO segue o modo noturno do sistema (nas centrais, costuma acompanhar o farol).
 */
@Composable
fun HeadUnitTheme(
    mode: ThemeMode = ThemeMode.DARK,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colors = colorSchemeFor(dark, highContrast)
    MaterialTheme(colorScheme = colors) {
        // Sem isto, textos fora de um Surface saem com a cor errada.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}
