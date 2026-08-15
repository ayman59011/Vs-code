package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CodeEditorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColorHex: String = "#007acc",
    content: @Composable () -> Unit
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(accentColorHex))
    } catch (e: Exception) {
        Color(0xFF007ACC) // Default VS Code blue
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            secondary = Color(0xFF252526), // VS Code Sidebar dark
            onSecondary = Color(0xFFD4D4D4),
            background = Color(0xFF1E1E1E), // VS Code Editor dark background
            onBackground = Color(0xFFD4D4D4),
            surface = Color(0xFF333333), // VS Code Activity Bar background
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2D2D30), // Editor tab inactive
            onSurfaceVariant = Color(0xFFD4D4D4),
            error = Color(0xFFF44336),
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            secondary = Color(0xFFF3F3F3), // VS Code Sidebar light
            onSecondary = Color(0xFF333333),
            background = Color.White, // VS Code Editor light background
            onBackground = Color(0xFF1E1E1E),
            surface = Color(0xFFE1E1E1), // VS Code Activity Bar light background
            onSurface = Color(0xFF333333),
            surfaceVariant = Color(0xFFECECEC),
            onSurfaceVariant = Color(0xFF1E1E1E),
            error = Color(0xFFD32F2F),
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
