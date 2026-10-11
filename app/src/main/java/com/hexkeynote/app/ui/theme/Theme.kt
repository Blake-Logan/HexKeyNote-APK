package com.hexkeynote.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HexKeyDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7C9BFF),
    onPrimary = Color(0xFF08111D),
    secondary = Color(0xFF8BE8C4),
    tertiary = Color(0xFFC7B8FF),
    background = Color(0xFF09111D),
    onBackground = Color(0xFFEAF2FF),
    surface = Color(0xFF101A2B),
    onSurface = Color(0xFFEAF2FF),
    surfaceVariant = Color(0xFF182539),
    outline = Color(0xFF2B3B58),
)

@Composable
fun HexKeyNoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HexKeyDarkColorScheme,
        typography = Typography(),
        content = content,
    )
}
