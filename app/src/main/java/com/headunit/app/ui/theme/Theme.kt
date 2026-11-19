package com.headunit.app.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val Colors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00222E),
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF0B0E13),
    onBackground = Color(0xFFE6E9EF),
    surface = Color(0xFF12161D),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF1B212A),
    onSurfaceVariant = Color(0xFFC3CAD6),
)

/** Tema escuro fixo: no carro, à noite, tela clara ofusca. */
@Composable
fun HeadUnitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors) {
        // Sem isto, textos fora de um Surface saem pretos sobre o fundo escuro.
        CompositionLocalProvider(LocalContentColor provides Colors.onBackground, content = content)
    }
}
