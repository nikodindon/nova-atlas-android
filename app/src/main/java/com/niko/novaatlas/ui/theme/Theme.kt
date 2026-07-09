package com.niko.novaatlas.ui.theme

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

/**
 * Theme Nova-Atlas (palette du site Flask).
 * Force darkColorScheme : le site est dark, on s'aligne.
 * Le dynamicColor (Android 12+) est desactive exprès pour garder l'identite visuelle.
 */
private val NovaAtlasDarkColorScheme = darkColorScheme(
    primary       = NovaAccentBlue,
    onPrimary     = NovaTextPrimary,
    primaryContainer    = NovaAccentBlue.copy(alpha = 0.15f),
    onPrimaryContainer  = NovaAccentBlueLight,

    secondary       = NovaAccentYellow,
    onSecondary     = NovaBg0,
    secondaryContainer    = NovaAccentYellow.copy(alpha = 0.15f),
    onSecondaryContainer  = NovaAccentYellow,

    tertiary        = NovaAccentGreen,
    onTertiary      = NovaBg0,

    background     = NovaBg0,
    onBackground   = NovaTextPrimary,

    surface        = NovaBg1,
    onSurface      = NovaTextPrimary,
    surfaceVariant = NovaBg2,
    onSurfaceVariant = NovaTextSecondary,

    surfaceContainerLowest  = NovaBg0,
    surfaceContainerLow     = NovaBg1,
    surfaceContainer        = NovaBg2,
    surfaceContainerHigh    = NovaBg3,
    surfaceContainerHighest = NovaBg3,

    outline        = NovaBg4,
    outlineVariant = NovaBg4.copy(alpha = 0.5f),

    error          = NovaAccentRed,
    onError        = NovaTextPrimary,

    inverseSurface = NovaTextPrimary,
    inverseOnSurface = NovaBg0,
)

@Composable
fun NovaAtlasTheme(
    darkTheme: Boolean = true,  // force dark (cohérent avec le site)
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NovaAtlasDarkColorScheme else NovaAtlasDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
