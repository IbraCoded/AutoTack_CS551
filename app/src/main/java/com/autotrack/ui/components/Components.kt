package com.autotrack.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.autotrack.navigation.Screen
import com.autotrack.ui.theme.AmberWarn
import com.autotrack.ui.theme.GreenSuccess
import com.autotrack.ui.theme.RedAlert

// Bottom Navigation Bar
data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: @Composable () -> Unit
)

@Composable
fun AutoTrackBottomBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Home", Screen.Home.route) { Icon(Icons.Filled.Home, null) },
        BottomNavItem("Records", Screen.Records.route) { Icon(Icons.Filled.Build, null) },
        BottomNavItem("Fuel", Screen.Fuel.route) { Icon(Icons.Filled.LocalGasStation, null) },
        BottomNavItem("Services", Screen.Services.route) { Icon(Icons.Filled.DateRange, null) },
        BottomNavItem("Analytics", Screen.Analytics.route) { Icon(Icons.Filled.BarChart, null) },
    )

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { item.icon() },
                label = { Text(item.label, fontSize = 10.sp) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

// Top App Bar
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
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            if (showSettings) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Preferences")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

// Vehicle Health Score Ring
@Composable
fun HealthScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    size: Float = 80f
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "healthScore"
    )
    val ringColor = when {
        score >= 70 -> GreenSuccess
        score >= 40 -> AmberWarn
        else -> RedAlert
    }
    val label = when {
        score >= 70 -> "Good"
        score >= 40 -> "Fair"
        else -> "Poor"
    }

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size * 0.12f
            val radius = (this.size.minDimension - strokeWidth) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)

            // Background arc
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = -220f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Score arc
            drawArc(
                color = ringColor,
                startAngle = -220f,
                sweepAngle = 260f * (animatedScore / 100f),
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                fontSize = (size * 0.22).sp,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
            Text(
                text = label,
                fontSize = (size * 0.13).sp,
                color = ringColor
            )
        }
    }
}

// Service urgency badge
@Composable
fun UrgencyBadge(daysUntilDue: Int) {
    val (text, color) = when {
        daysUntilDue < 0 -> "OVERDUE" to RedAlert
        daysUntilDue <= 7 -> "DUE SOON" to RedAlert
        daysUntilDue <= 30 -> "UPCOMING" to AmberWarn
        else -> "OK" to GreenSuccess
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// Loading indicator
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}