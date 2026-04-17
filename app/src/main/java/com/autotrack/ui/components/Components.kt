package com.autotrack.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.autotrack.navigation.Screen
import com.autotrack.ui.theme.*

// ─── Bottom Navigation Bar ────────────────────────────────────
data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: @Composable () -> Unit
)

@Composable
fun AutoTrackBottomBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Home",      Screen.Home.route)      { Icon(Icons.Filled.Home, null) },
        BottomNavItem("Records",   Screen.Records.route)   { Icon(Icons.Filled.Build, null) },
        BottomNavItem("Fuel",      Screen.Fuel.route)      { Icon(Icons.Filled.LocalGasStation, null) },
        BottomNavItem("Services",  Screen.Services.route)  { Icon(Icons.Filled.DateRange, null) },
        BottomNavItem("Analytics", Screen.Analytics.route) { Icon(Icons.Filled.BarChart, null) },
    )
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    NavigationBar(
        containerColor = GunmetalDeep,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon  = { item.icon() },
                label = {
                    Text(
                        item.label,
                        fontSize     = 10.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight   = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                selected = selected,
                onClick  = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = GoldPrimary,
                    selectedTextColor   = GoldPrimary,
                    unselectedIconColor = SilverDim,
                    unselectedTextColor = SilverDim,
                    indicatorColor      = GunmetalMid
                )
            )
        }
    }
}

// ─── Top App Bar ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTrackTopBar(
    title: String,
    showBack: Boolean = false,
    showSettings: Boolean = false,
    onBack: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                title,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontSize      = 15.sp,
                color         = ChromeWhite
            )
        },
        navigationIcon = {
            if (showBack) IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
            }
        },
        actions = {
            if (showSettings) IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Preferences", tint = SilverMid)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor         = GunmetalDeep,
            titleContentColor      = ChromeWhite,
            navigationIconContentColor = GoldPrimary,
            actionIconContentColor = SilverMid
        )
    )
}

// ─── Premium Divider with gold accent ────────────────────────
@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        GoldDim.copy(alpha = 0.5f),
                        GoldPrimary.copy(alpha = 0.8f),
                        GoldDim.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )
            )
    )
}

// ─── Vehicle Health Score Ring ────────────────────────────────
@Composable
fun HealthScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    size: Float = 80f
) {
    val animatedScore by animateFloatAsState(
        targetValue  = score.toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic),
        label        = "healthScore"
    )
    val (ringColor, glowColor, label) = when {
        score >= 70 -> Triple(EmeraldSuccess, EmeraldSuccess.copy(alpha = 0.3f), "GOOD")
        score >= 40 -> Triple(AmberWarn,      AmberWarn.copy(alpha = 0.3f),      "FAIR")
        else        -> Triple(CrimsonAlert,   CrimsonAlert.copy(alpha = 0.3f),   "POOR")
    }

    Box(
        modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth  = size * 0.10f
            val glowWidth    = size * 0.20f
            val radius       = (this.size.minDimension - strokeWidth) / 2
            val center       = Offset(this.size.width / 2, this.size.height / 2)
            val arcTopLeft   = Offset(center.x - radius, center.y - radius)
            val arcSize      = Size(radius * 2, radius * 2)

            // Track
            drawArc(
                color      = GunmetalLight,
                startAngle = -220f, sweepAngle = 260f,
                useCenter  = false,
                topLeft    = arcTopLeft, size = arcSize,
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Glow halo
            val sweep = 260f * (animatedScore / 100f)
            drawArc(
                color      = glowColor,
                startAngle = -220f, sweepAngle = sweep,
                useCenter  = false,
                topLeft    = Offset(center.x - radius, center.y - radius),
                size       = Size(radius * 2, radius * 2),
                style      = Stroke(width = glowWidth, cap = StrokeCap.Round)
            )
            // Main arc
            drawArc(
                color      = ringColor,
                startAngle = -220f, sweepAngle = sweep,
                useCenter  = false,
                topLeft    = arcTopLeft, size = arcSize,
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = score.toString(),
                fontSize   = (size * 0.24).sp,
                fontWeight = FontWeight.Black,
                color      = ringColor
            )
            Text(
                text       = label,
                fontSize   = (size * 0.11).sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color      = ringColor.copy(alpha = 0.8f)
            )
        }
    }
}

// ─── Service urgency badge ────────────────────────────────────
@Composable
fun UrgencyBadge(daysUntilDue: Int) {
    val (text, color) = when {
        daysUntilDue < 0   -> "OVERDUE"  to CrimsonAlert
        daysUntilDue <= 7  -> "DUE SOON" to CrimsonAlert
        daysUntilDue <= 30 -> "UPCOMING" to AmberWarn
        else               -> "OK"       to EmeraldSuccess
    }
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.4f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text          = text,
            color         = color,
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

// ─── Loading indicator ────────────────────────────────────────
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = GoldPrimary, strokeWidth = 2.dp)
    }
}

// ─── Premium stat chip ────────────────────────────────────────
@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier
            .background(GunmetalMid, RoundedCornerShape(10.dp))
            .border(1.dp, GunmetalLight, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text          = value,
            fontWeight    = FontWeight.Bold,
            fontSize      = 15.sp,
            color         = ChromeWhite
        )
        Text(
            text          = label,
            fontSize      = 9.sp,
            letterSpacing = 1.sp,
            color         = SilverDim
        )
    }
}
