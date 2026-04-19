package com.autotrack.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.autotrack.navigation.Screen
import com.autotrack.ui.theme.*

// ─── Theme helper: read LocalIsDarkTheme (set from prefs, not system) ──
@Composable
fun isDark() = LocalIsDarkTheme.current

// ─── Lightweight carbon-fibre background ─────────────────────────────
// Uses a simple remembered Paint to draw a repeating 8×8 diamond tile.
// This is far cheaper than the previous DrawScope loop (no ANR risk).
fun Modifier.carbonFibreBackground(dark: Boolean): Modifier {
    val tile1 = if (dark) Color(0xFF12151E) else Color(0xFFF0F2F5)
    val tile2 = if (dark) Color(0xFF0D1018) else Color(0xFFE8EAED)
    val hi    = if (dark) Color(0xFF1E2330) else Color(0xFFD8DCE4)
    return this.drawBehind {
        // Fill base
        drawRect(tile1)
        // Draw a simple repeating 16×16 woven grid
        val cell = 16.dp.toPx()
        val half = cell / 2f
        var y = 0f
        var row = 0
        while (y < size.height + cell) {
            var x = 0f
            var col = 0
            while (x < size.width + cell) {
                val even = (row + col) % 2 == 0
                // alternate tile colour for weave
                drawRect(
                    color   = if (even) tile1 else tile2,
                    topLeft = Offset(x, y),
                    size    = Size(half, half)
                )
                drawRect(
                    color   = if (even) tile2 else tile1,
                    topLeft = Offset(x + half, y + half),
                    size    = Size(half, half)
                )
                // subtle highlight on top edge
                drawRect(
                    color   = hi,
                    topLeft = Offset(x, y),
                    size    = Size(half, 1.dp.toPx())
                )
                x += cell
                col++
            }
            y += half
            row++
        }
    }
}

// ─── Bottom Navigation Bar ────────────────────────────────────────────
data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: @Composable () -> Unit
)

@Composable
fun AutoTrackBottomBar(navController: NavController) {
    val dark = isDark()
    val items = listOf(
        BottomNavItem("Home",      Screen.Home.route)      { Icon(Icons.Filled.Home, null) },
        BottomNavItem("Records",   Screen.Records.route)   { Icon(Icons.Filled.Build, null) },
        BottomNavItem("Fuel",      Screen.Fuel.route)      { Icon(Icons.Filled.LocalGasStation, null) },
        BottomNavItem("Services",  Screen.Services.route)  { Icon(Icons.Filled.DateRange, null) },
        BottomNavItem("Analytics", Screen.Analytics.route) { Icon(Icons.Filled.BarChart, null) },
    )
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val barBg    = if (dark) DarkSurface else LightSurface
    val divColor = if (dark) DarkCardBorder else LightCardBorder
    val amber    = if (dark) AmberDark else AmberLight
    val inactive = if (dark) DarkTextMuted else LightTextMuted

    Column {
        HorizontalDivider(color = divColor, thickness = 0.5.dp)
        NavigationBar(
            containerColor = barBg,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    icon = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            item.icon()
                            Spacer(Modifier.height(2.dp))
                            // Amber dot pip under active item
                            Box(
                                Modifier
                                    .size(width = 16.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (selected) amber else Color.Transparent)
                            )
                        }
                    },
                    label = {
                        Text(
                            item.label,
                            fontSize      = 10.sp,
                            letterSpacing = 0.3.sp,
                            fontWeight    = if (selected) FontWeight.SemiBold else FontWeight.Normal
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
                        selectedIconColor   = amber,
                        selectedTextColor   = amber,
                        unselectedIconColor = inactive,
                        unselectedTextColor = inactive,
                        indicatorColor      = Color.Transparent
                    )
                )
            }
        }
    }
}

// ─── Top App Bar ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTrackTopBar(
    title: String,
    showBack: Boolean = false,
    showSettings: Boolean = false,
    onBack: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val dark    = isDark()
    val barBg   = if (dark) DarkSurface else LightSurface
    val amber   = if (dark) AmberDark   else AmberLight
    val onColor = if (dark) DarkTextPrimary else LightTextPrimary
    val sub     = if (dark) DarkTextSecondary else LightTextSecondary

    TopAppBar(
        title = {
            Text(title, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, fontSize = 15.sp, color = onColor)
        },
        navigationIcon = {
            if (showBack) IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = amber)
            }
        },
        actions = {
            if (showSettings) IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, "Preferences", tint = sub)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor             = barBg,
            titleContentColor          = onColor,
            navigationIconContentColor = amber,
            actionIconContentColor     = sub
        )
    )
}

// ─── Gold / Amber Divider ──────────────────────────────────────────────
@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    val amber = if (isDark()) AmberDark else AmberLight
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, amber.copy(alpha = 0.5f), amber.copy(alpha = 0.8f),
                        amber.copy(alpha = 0.5f), Color.Transparent)
                )
            )
    )
}

// ─── Vehicle Health Score Ring ─────────────────────────────────────────
@Composable
fun HealthScoreRing(score: Int, modifier: Modifier = Modifier, size: Float = 80f) {
    val dark = isDark()
    val animatedScore by animateFloatAsState(
        targetValue   = score.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label         = "healthScore"
    )
    val ringColor = when {
        score >= 70 -> if (dark) GreenDark else GreenAccent
        score >= 40 -> if (dark) AmberDark else AmberLight
        else        -> if (dark) RedDark   else RedAccent
    }
    val label      = when { score >= 70 -> "GOOD"; score >= 40 -> "FAIR"; else -> "POOR" }
    val trackColor = if (dark) DarkCardBorder else LightCardBorder

    Box(modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw       = size * 0.10f
            val gw       = size * 0.18f
            val radius   = (this.size.minDimension - sw) / 2
            val cx       = this.size.width / 2
            val cy       = this.size.height / 2
            val tl       = Offset(cx - radius, cy - radius)
            val arcSize  = Size(radius * 2, radius * 2)
            val sweep    = 260f * (animatedScore / 100f)

            drawArc(trackColor, -220f, 260f, false, tl, arcSize,
                style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(ringColor.copy(alpha = 0.2f), -220f, sweep, false, tl, arcSize,
                style = Stroke(gw, cap = StrokeCap.Round))
            drawArc(ringColor, -220f, sweep, false, tl, arcSize,
                style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toString(), fontSize = (size * 0.24).sp, fontWeight = FontWeight.Black, color = ringColor)
            Text(label, fontSize = (size * 0.11).sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp, color = ringColor.copy(alpha = 0.8f))
        }
    }
}

// ─── Service urgency badge ─────────────────────────────────────────────
@Composable
fun UrgencyBadge(daysUntilDue: Int) {
    val dark = isDark()
    val (text, color) = when {
        daysUntilDue < 0   -> "OVERDUE"  to (if (dark) RedDark  else RedAccent)
        daysUntilDue <= 7  -> "DUE SOON" to (if (dark) RedDark  else RedAccent)
        daysUntilDue <= 30 -> "UPCOMING" to (if (dark) AmberDark else AmberLight)
        else               -> "OK"       to (if (dark) GreenDark else GreenAccent)
    }
    Box(
        Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}

// ─── Loading indicator ─────────────────────────────────────────────────
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    val amber = if (isDark()) AmberDark else AmberLight
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = amber, strokeWidth = 2.dp)
    }
}

// ─── Stat chip ─────────────────────────────────────────────────────────
@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    val dark       = isDark()
    val bg         = if (dark) DarkCard  else Color(0xFFF1F5F9)
    val borderClr  = if (dark) DarkCardBorder else LightCardBorder
    val valueColor = if (dark) DarkTextPrimary else LightTextPrimary
    val labelColor = if (dark) DarkTextMuted   else LightTextMuted

    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, borderClr, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = valueColor)
        Spacer(Modifier.height(1.dp))
        Text(label, fontSize = 9.sp, letterSpacing = 0.8.sp, color = labelColor, fontWeight = FontWeight.Medium)
    }
}

// ─── KPI tile (Analytics grid) ─────────────────────────────────────────
@Composable
fun KpiTile(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    val dark    = isDark()
    val bg      = if (dark) DarkCard      else LightCard
    val borderC = if (dark) DarkCardBorder else LightCardBorder
    val onColor = if (dark) DarkTextPrimary else LightTextPrimary
    val sub     = if (dark) DarkTextSecondary else LightTextSecondary

    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .border(1.dp, borderC, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Box(Modifier.size(width = 24.dp, height = 4.dp).background(accentColor, RoundedCornerShape(2.dp)))
        Spacer(Modifier.height(10.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = onColor)
        Spacer(Modifier.height(2.dp))
        Text(label.uppercase(), fontSize = 9.sp, letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold, color = sub)
    }
}

// ─── Preference text field colours ────────────────────────────────────
@Composable
fun premiumTextFieldColors(): TextFieldColors {
    val dark  = isDark()
    val amber = if (dark) AmberDark else AmberLight
    val on    = if (dark) DarkTextPrimary else LightTextPrimary
    val sub   = if (dark) DarkTextSecondary else LightTextSecondary
    val bg    = if (dark) DarkCard else Color(0xFFF8FAFC)
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = amber,
        unfocusedBorderColor    = if (dark) DarkCardBorder else LightCardBorder,
        focusedTextColor        = on,
        unfocusedTextColor      = on,
        cursorColor             = amber,
        focusedLabelColor       = amber,
        unfocusedLabelColor     = sub,
        unfocusedContainerColor = bg,
        focusedContainerColor   = bg
    )
}

// ─── Section label ─────────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val amber = if (isDark()) AmberDark else AmberLight
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = amber,
        letterSpacing = 2.sp, modifier = modifier.padding(start = 2.dp, bottom = 6.dp, top = 4.dp)
    )
}