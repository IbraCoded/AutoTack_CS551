package com.autotrack.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── CompositionLocal: every composable reads the USER pref not system theme
val LocalIsDarkTheme = compositionLocalOf { false }

// ── Dark Mode Palette ─────────────────────────────────────────────────
val DarkBg          = Color(0xFF0F1117)
val DarkSurface     = Color(0xFF181C27)
val DarkCard        = Color(0xFF1E2335)
val DarkCardBorder  = Color(0xFF2A3050)

// ── Light Mode Palette ────────────────────────────────────────────────
val LightBg         = Color(0xFFF8FAFC)
val LightSurface    = Color(0xFFFFFFFF)
val LightCard       = Color(0xFFFFFFFF)
val LightCardBorder = Color(0xFFE2E8F0)

// ── Accent Colours ────────────────────────────────────────────────────
val AmberDark    = Color(0xFFF59E0B)
val AmberDarkDim = Color(0xFF92400E)
val AmberLight   = Color(0xFFD97706)
val AmberLightDim= Color(0xFFFEF3C7)
val GreenAccent  = Color(0xFF059669)
val GreenDark    = Color(0xFF10B981)
val RedAccent    = Color(0xFFDC2626)
val RedDark      = Color(0xFFEF4444)

// ── Text ──────────────────────────────────────────────────────────────
val DarkTextPrimary   = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextMuted     = Color(0xFF475569)
val LightTextPrimary   = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted     = Color(0xFF94A3B8)

// ── Backward-compat aliases ───────────────────────────────────────────
val Obsidian       = DarkBg
val GunmetalDeep   = DarkSurface
val GunmetalMid    = DarkCard
val GunmetalLight  = DarkCardBorder
val GoldPrimary    = AmberDark
val GoldLight      = Color(0xFFFCD34D)
val GoldDim        = AmberDarkDim
val ChromeWhite    = DarkTextPrimary
val SilverMid      = DarkTextSecondary
val SilverDim      = DarkTextMuted
val EmeraldSuccess = GreenDark
val AmberWarn      = AmberDark
val CrimsonAlert   = RedDark
val GreenSuccess   = GreenDark
val RedAlert       = RedDark

private val DarkColors = darkColorScheme(
    primary              = AmberDark,
    onPrimary            = DarkBg,
    primaryContainer     = Color(0xFF292108),
    onPrimaryContainer   = Color(0xFFFCD34D),
    secondary            = DarkTextSecondary,
    onSecondary          = DarkBg,
    secondaryContainer   = DarkCard,
    onSecondaryContainer = DarkTextPrimary,
    background           = DarkBg,
    surface              = DarkSurface,
    surfaceVariant       = DarkCard,
    onBackground         = DarkTextPrimary,
    onSurface            = DarkTextPrimary,
    onSurfaceVariant     = DarkTextSecondary,
    error                = RedDark,
    onError              = Color.White,
    outline              = DarkCardBorder,
    outlineVariant       = DarkTextMuted
)

private val LightColors = lightColorScheme(
    primary              = AmberLight,
    onPrimary            = Color.White,
    primaryContainer     = AmberLightDim,
    onPrimaryContainer   = Color(0xFF78350F),
    secondary            = LightTextSecondary,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFF1F5F9),
    onSecondaryContainer = LightTextPrimary,
    background           = LightBg,
    surface              = LightSurface,
    surfaceVariant       = Color(0xFFF1F5F9),
    onBackground         = LightTextPrimary,
    onSurface            = LightTextPrimary,
    onSurfaceVariant     = LightTextSecondary,
    error                = RedAccent,
    onError              = Color.White,
    outline              = LightCardBorder,
    outlineVariant       = Color(0xFFCBD5E1)
)

val AutoTrackTypography = Typography(
    titleLarge   = TextStyle(fontWeight = FontWeight.Bold,    letterSpacing = 0.sp,    fontSize = 22.sp),
    titleMedium  = TextStyle(fontWeight = FontWeight.SemiBold,letterSpacing = 0.15.sp, fontSize = 16.sp),
    titleSmall   = TextStyle(fontWeight = FontWeight.Medium,  letterSpacing = 0.1.sp,  fontSize = 14.sp),
    bodyLarge    = TextStyle(fontWeight = FontWeight.Normal,  letterSpacing = 0.5.sp,  fontSize = 16.sp),
    bodyMedium   = TextStyle(fontWeight = FontWeight.Normal,  letterSpacing = 0.25.sp, fontSize = 14.sp),
    bodySmall    = TextStyle(fontWeight = FontWeight.Normal,  letterSpacing = 0.4.sp,  fontSize = 12.sp),
    labelLarge   = TextStyle(fontWeight = FontWeight.SemiBold,letterSpacing = 1.2.sp,  fontSize = 11.sp),
    labelMedium  = TextStyle(fontWeight = FontWeight.Medium,  letterSpacing = 1.5.sp,  fontSize = 10.sp),
    labelSmall   = TextStyle(fontWeight = FontWeight.Medium,  letterSpacing = 1.5.sp,  fontSize = 9.sp),
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
            window.statusBarColor = if (darkTheme) DarkBg.toArgb() else LightBg.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }
    // Provide LocalIsDarkTheme so all screens read the USER preference
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AutoTrackTypography,
            content     = content
        )
    }
}