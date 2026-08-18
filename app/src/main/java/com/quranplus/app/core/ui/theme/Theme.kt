package com.quranplus.app.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = QuranColors.Primary,
    onPrimary = QuranColors.OnPrimary,
    primaryContainer = QuranColors.PrimaryContainer,
    onPrimaryContainer = QuranColors.OnPrimaryContainer,
    secondary = QuranColors.Secondary,
    onSecondary = QuranColors.OnSecondary,
    secondaryContainer = QuranColors.SecondaryContainer,
    onSecondaryContainer = QuranColors.OnSecondaryContainer,
    tertiary = QuranColors.Tertiary,
    onTertiary = QuranColors.OnTertiary,
    tertiaryContainer = QuranColors.TertiaryContainer,
    onTertiaryContainer = QuranColors.OnTertiaryContainer,
    background = QuranColors.BackgroundDark,
    onBackground = QuranColors.OnSurfaceDark,
    surface = QuranColors.SurfaceDark,
    onSurface = QuranColors.OnSurfaceDark,
    surfaceVariant = QuranColors.SurfaceVariantDark,
    onSurfaceVariant = QuranColors.OnSurfaceVariantDark,
    outline = QuranColors.OutlineDark,
    outlineVariant = QuranColors.OutlineVariantDark,
    error = QuranColors.Error,
    onError = QuranColors.OnPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = QuranColors.Primary,
    onPrimary = QuranColors.OnPrimary,
    primaryContainer = QuranColors.PrimaryContainer,
    onPrimaryContainer = QuranColors.OnPrimaryContainer,
    secondary = QuranColors.Secondary,
    onSecondary = QuranColors.OnSecondary,
    secondaryContainer = QuranColors.SecondaryContainer,
    onSecondaryContainer = QuranColors.OnSecondaryContainer,
    tertiary = QuranColors.Tertiary,
    onTertiary = QuranColors.OnTertiary,
    tertiaryContainer = QuranColors.TertiaryContainer,
    onTertiaryContainer = QuranColors.OnTertiaryContainer,
    background = QuranColors.BackgroundLight,
    onBackground = QuranColors.OnSurfaceLight,
    surface = QuranColors.SurfaceLightMode,
    onSurface = QuranColors.OnSurfaceLight,
    surfaceVariant = QuranColors.SurfaceVariantLight,
    onSurfaceVariant = QuranColors.OnSurfaceVariantLight,
    outline = QuranColors.OutlineLight,
    outlineVariant = QuranColors.OutlineVariantLight,
    error = QuranColors.Error,
    onError = QuranColors.OnPrimary
)

@Composable
fun QuranPlusTheme(
    darkTheme: Boolean = true, // Dark mode is default per DESIGN.md
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuranPlusTypography,
        shapes = QuranPlusShapes,
        content = content
    )
}
