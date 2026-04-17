package com.autotrack.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── Premium Palette ─────────────────────────────────────────────
val Obsidian        = Color(0xFF0D0F14)
val GunmetalDeep    = Color(0xFF131720)
val GunmetalMid     = Color(0xFF1C2233)
val GunmetalLight   = Color(0xFF252D42)

val GoldPrimary     = Color(0xFFC9A84C)
val GoldLight       = Color(0xFFE8C97A)
val GoldDim         = Color(0xFF8A6E2F)

val ChromeWhite     = Color(0xFFF0F2F7)
val SilverMid       = Color(0xFFADB5C8)
val SilverDim       = Color(0xFF5A6380)

val EmeraldSuccess  = Color(0xFF2ECC8E)
val AmberWarn       = Color(0xFFE8A020)
val CrimsonAlert    = Color(0xFFE03C52)

// Keep old names for backward compat in other files
val GreenSuccess    = EmeraldSuccess
val RedAlert        = CrimsonAlert

private val DarkPremiumColors = darkColorScheme(
    primary              = GoldPrimary,
    onPrimary            = Obsidian,
    primaryContainer     = GunmetalMid,
    onPrimaryContainer   = GoldLight,
    secondary            = SilverMid,
    onSecondary          = Obsidian,
    secondaryContainer   = GunmetalLight,
    onSecondaryContainer = ChromeWhite,
    background           = Obsidian,
    surface              = GunmetalDeep,
    surfaceVariant       = GunmetalMid,
    onBackground         = ChromeWhite,
    onSurface            = ChromeWhite,
    onSurfaceVariant     = SilverMid,
    error                = CrimsonAlert,
    onError              = ChromeWhite,
    outline              = GunmetalLight,
    outlineVariant       = SilverDim
)

private val LightPremiumColors = lightColorScheme(
    primary              = Color(0xFF8A6820),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFF5E9C8),
    onPrimaryContainer   = Color(0xFF3D2D00),
    secondary            = Color(0xFF6B5E45),
    onSecondary          = Color.White,
    background           = Color(0xFFF7F5F0),
    surface              = Color(0xFFFFFFFF),
    surfaceVariant       = Color(0xFFF0EBE0),
    onBackground         = Color(0xFF1A1710),
    onSurface            = Color(0xFF1A1710),
    onSurfaceVariant     = Color(0xFF5A5040),
    error                = CrimsonAlert,
    outline              = Color(0xFFD4C8AA),
    outlineVariant       = Color(0xFFBEB0A0)
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
    val colorScheme = if (darkTheme) DarkPremiumColors else LightPremiumColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) Obsidian.toArgb()
            else Color(0xFFF7F5F0).toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AutoTrackTypography,
        content     = content
    )
}
