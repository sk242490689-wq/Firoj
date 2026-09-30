package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JarvisColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = DeepNavyBg,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = CyberCyan,
    secondary = ElectricBlue,
    onSecondary = DeepNavyBg,
    secondaryContainer = DarkSurfaceCard,
    onSecondaryContainer = TextPrimary,
    tertiary = StarkGold,
    onTertiary = DeepNavyBg,
    background = DeepNavyBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = HudBorderCyan,
    outlineVariant = HudBorderBright,
    error = StarkRedAlert,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Always enforce the high-tech JARVIS HUD theme
    content: @Composable () -> Unit,
) {
    val colorScheme = JarvisColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DeepNavyBg.toArgb()
                window.navigationBarColor = DeepNavyBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
