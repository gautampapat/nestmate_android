package com.nestmate.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NestMateDarkColors = darkColorScheme(
    primary            = PurplePrimary,
    onPrimary          = Color.White,
    primaryContainer   = PurpleContainer,
    onPrimaryContainer = OnPurpleContainer,

    secondary          = AccentCyan,
    onSecondary        = Color(0xFF001F26),
    secondaryContainer = Color(0xFF003640),
    onSecondaryContainer = AccentCyanLight,

    tertiary           = PurpleLight,
    onTertiary         = Color(0xFF1A0050),

    background         = BackgroundDark,
    onBackground       = TextPrimary,

    surface            = SurfaceDark,
    onSurface          = TextPrimary,
    surfaceVariant     = SurfaceElevated,
    onSurfaceVariant   = TextSecondary,

    // M3 surface container hierarchy for layered surfaces
    surfaceContainerLowest  = SurfaceContainerLowest,
    surfaceContainer        = SurfaceContainer,
    surfaceContainerHigh    = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,

    error              = ErrorRed,
    onError            = Color.White,
    errorContainer     = Color(0xFF3D1111),
    onErrorContainer   = Color(0xFFFFDAD6),

    outline            = GlassBorder,
    outlineVariant     = Color(0xFF2A2A3E),

    inverseSurface     = Color(0xFFE4E1E9),
    inverseOnSurface   = Color(0xFF1A1A2E),
    inversePrimary     = PurplePrimaryDark
)

private val NestMateLightColors = lightColorScheme(
    primary            = PurplePrimaryDark,
    onPrimary          = Color.White,
    primaryContainer   = SurfaceVariantLight,
    onPrimaryContainer = PurplePrimaryDark,

    secondary          = AccentCyan,
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFCCF0F8),
    onSecondaryContainer = Color(0xFF001F26),

    background         = BackgroundLight,
    onBackground       = OnSurfaceLight,

    surface            = SurfaceLight,
    onSurface          = OnSurfaceLight,
    surfaceVariant     = SurfaceVariantLight,
    onSurfaceVariant   = OnSurfaceVariantLight,

    error              = ErrorRed,
    onError            = Color.White,

    outline            = Color(0xFFCBC4E8)
)

@Composable
fun NestMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NestMateDarkColors else NestMateLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}