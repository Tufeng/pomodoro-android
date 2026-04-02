package com.flowcycle.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PomodoroRed,
    onPrimary = LabelPrimaryDark,
    primaryContainer = PomodoroRedDark,
    secondary = BreakBlue,
    tertiary = LongBreakPurple,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = Surface2Dark,
    onBackground = LabelPrimaryDark,
    onSurface = LabelPrimaryDark,
    outline = SeparatorDark
)

private val LightColorScheme = lightColorScheme(
    primary = PomodoroRed,
    onPrimary = LabelPrimaryDark,
    primaryContainer = PomodoroRedLight,
    secondary = BreakBlue,
    tertiary = LongBreakPurple,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = BackgroundLight,
    onBackground = LabelPrimary,
    onSurface = LabelPrimary,
    outline = SeparatorLight
)

@Composable
fun FlowCycleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FlowCycleTypography,
        content = content
    )
}
