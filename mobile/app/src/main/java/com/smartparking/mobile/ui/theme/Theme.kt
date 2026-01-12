package com.smartparking.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Slate50,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo700,
    secondary = Emerald600,
    onSecondary = Slate50,
    secondaryContainer = Emerald100,
    onSecondaryContainer = Emerald700,
    tertiary = Amber500,
    onTertiary = Slate900,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber600,
    error = Rose600,
    onError = Slate50,
    errorContainer = Rose100,
    onErrorContainer = Rose700,
    background = Slate50,
    onBackground = Slate900,
    surface = Slate50,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate300,
    outlineVariant = Slate200
)

private val DarkColorScheme = darkColorScheme(
    primary = Indigo500,
    onPrimary = Slate900,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo100,
    secondary = Emerald500,
    onSecondary = Slate900,
    secondaryContainer = Emerald700,
    onSecondaryContainer = Emerald100,
    tertiary = Amber500,
    onTertiary = Slate900,
    tertiaryContainer = Amber600,
    onTertiaryContainer = Amber100,
    error = Rose500,
    onError = Slate900,
    errorContainer = Rose700,
    onErrorContainer = Rose100,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    outline = Slate600,
    outlineVariant = Slate700
)

@Composable
fun SmartParkingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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
        typography = Typography,
        content = content
    )
}
