package com.autotrack.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

//Palette
val NavyBlue     = Color(0xFF1B3A6B)
val MidBlue      = Color(0xFF2E75B6)
val LightBlue    = Color(0xFFD6E4F0)
val GreenSuccess = Color(0xFF1E7A4A)
val AmberWarn    = Color(0xFFF59E0B)
val RedAlert     = Color(0xFFDC2626)

private val LightColors = lightColorScheme(
    primary            = NavyBlue,
    onPrimary          = Color.White,
    primaryContainer   = LightBlue,
    onPrimaryContainer = NavyBlue,
    secondary          = MidBlue,
    onSecondary        = Color.White,
    background         = Color(0xFFF8FAFC),
    surface            = Color.White,
    onBackground       = Color(0xFF1A1A2E),
    onSurface          = Color(0xFF1A1A2E),
    error              = RedAlert,
    outline            = Color(0xFFCBD5E1)
)

private val DarkColors = darkColorScheme(
    primary            = LightBlue,
    onPrimary          = NavyBlue,
    primaryContainer   = Color(0xFF1E3A5F),
    onPrimaryContainer = LightBlue,
    secondary          = MidBlue,
    onSecondary        = Color.White,
    background         = Color(0xFF0F172A),
    surface            = Color(0xFF1E293B),
    onBackground       = Color(0xFFE2E8F0),
    onSurface          = Color(0xFFE2E8F0),
    error              = Color(0xFFFF6B6B),
    outline            = Color(0xFF334155)
)

@Composable
fun AutoTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}