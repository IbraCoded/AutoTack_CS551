package com.autotrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.autotrack.ui.components.*
import com.autotrack.ui.theme.*
import com.autotrack.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    navController: NavController,
    vm: MainViewModel = hiltViewModel()
) {
    val dark = isDark()
    val prefs by vm.preferences.collectAsStateWithLifecycle()

    var intervalExpanded by remember { mutableStateOf(false) }
    var distanceExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    var thresholdText by remember(prefs) { mutableStateOf(prefs.overdueThresholdDays.toString()) }

    val intervals = listOf("Daily", "Weekly", "Fortnightly", "Monthly")
    val distUnits = listOf("mi", "km")
    val currencies = listOf("GBP £", "USD $", "EUR €")

    val bgPage = if (dark) DarkBg else LightBg
    val on = if (dark) DarkTextPrimary else LightTextPrimary
    val sub = if (dark) DarkTextSecondary else LightTextSecondary
    val muted = if (dark) DarkTextMuted else LightTextMuted
    val amber = if (dark) AmberDark else AmberLight
    val cardBg = if (dark) DarkCard else LightCard
    val bord = if (dark) DarkCardBorder else LightCardBorder

    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title = "PREFERENCES",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = bgPage
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .carbonFibreBackground(dark)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── APPEARANCE ───────────────────────────────────────────
            item { SectionLabel("APPEARANCE") }

            item {
                PrefCard(cardBg, bord) {
                    PrefSwitchRow("Dark Theme", Icons.Filled.DarkMode, amber, prefs.darkTheme, dark, on) {
                        vm.setDarkTheme(it)
                    }
                    PrefDividerLine(bord)

                    ExposedDropdownMenuBox(distanceExpanded, { distanceExpanded = it }) {
                        PrefDropdownRow(
                            "Distance Unit",
                            Icons.Filled.Speed,
                            amber,
                            prefs.distanceUnit,
                            distanceExpanded,
                            on,
                            Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(distanceExpanded, { distanceExpanded = false }, Modifier.background(cardBg)) {
                            distUnits.forEach { u ->
                                DropdownMenuItem({ Text(u, color = on) }, { vm.setDistanceUnit(u); distanceExpanded = false })
                            }
                        }
                    }
                    PrefDividerLine(bord)

                    ExposedDropdownMenuBox(currencyExpanded, { currencyExpanded = it }) {
                        PrefDropdownRow(
                            "Currency",
                            Icons.Filled.AttachMoney,
                            amber,
                            prefs.currency,
                            currencyExpanded,
                            on,
                            Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(currencyExpanded, { currencyExpanded = false }, Modifier.background(cardBg)) {
                            currencies.forEach { c ->
                                DropdownMenuItem({ Text(c, color = on) }, { vm.setCurrency(c); currencyExpanded = false })
                            }
                        }
                    }
                }
            }

            // ── NOTIFICATIONS ────────────────────────────────────────
            item { Spacer(Modifier.height(4.dp)); SectionLabel("NOTIFICATIONS") }

            item {
                PrefCard(cardBg, bord) {
                    val blueIcon = if (dark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                    PrefSwitchRow("Service Reminders", Icons.Filled.Notifications, blueIcon, prefs.remindersEnabled, dark, on) {
                        vm.setRemindersEnabled(it)
                    }

                    if (prefs.remindersEnabled) {
                        PrefDividerLine(bord)
                        ExposedDropdownMenuBox(intervalExpanded, { intervalExpanded = it }) {
                            PrefDropdownRow(
                                "Reminder Interval",
                                Icons.Filled.Schedule,
                                blueIcon,
                                prefs.reminderInterval,
                                intervalExpanded,
                                on,
                                Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(intervalExpanded, { intervalExpanded = false }, Modifier.background(cardBg)) {
                                intervals.forEach { i ->
                                    DropdownMenuItem({ Text(i, color = on) }, { vm.setReminderInterval(i); intervalExpanded = false })
                                }
                            }
                        }
                    }

                    PrefDividerLine(bord)
                    val greenIcon = if (dark) GreenDark else GreenAccent
                    PrefSwitchRow("Mileage Alerts", Icons.Filled.Speed, greenIcon, prefs.mileageAlertsEnabled, dark, on) {
                        vm.setMileageAlerts(it)
                    }

                    if (prefs.mileageAlertsEnabled) {
                        PrefDividerLine(bord)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Overdue threshold (days)", color = sub, fontSize = 13.sp)
                            OutlinedTextField(
                                value = thresholdText,
                                onValueChange = {
                                    thresholdText = it
                                    it.toIntOrNull()?.let { d -> vm.setOverdueThreshold(d) }
                                },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                colors = premiumTextFieldColors()
                            )
                        }
                    }
                }
            }

            // ── SENSORS ──────────────────────────────────────────────
            item { Spacer(Modifier.height(4.dp)); SectionLabel("SENSORS") }

            item {
                PrefCard(cardBg, bord) {
                    val purpleIcon = if (dark) Color(0xFFC084FC) else Color(0xFF7C3AED)
                    PrefSwitchRow("Shake to Log Service", Icons.Filled.Vibration, purpleIcon, prefs.shakeEnabled, dark, on) {
                        vm.setShakeEnabled(it)
                    }
                    PrefDividerLine(bord)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, null, tint = muted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Shake your phone to quickly open the Log Service screen",
                            color = muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ── ABOUT ────────────────────────────────────────────────
            item { Spacer(Modifier.height(4.dp)); SectionLabel("ABOUT") }

            item {
                PrefCard(cardBg, bord) {
                    AboutInfoRow("Version", "1.0.0", on, sub)
                    PrefDividerLine(bord)
                    AboutInfoRow("Build", "CS551", on, sub)
                    PrefDividerLine(bord)
                    AboutInfoRow("Data Storage", "Room DB", on, sub)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Reusable components (kept exactly as you had them, only fixed Rows) ─────────────────────────────

@Composable
fun PrefCard(bg: Color, border: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp)),
        content = content
    )
}

@Composable
fun PrefDividerLine(bord: Color) {
    HorizontalDivider(
        color = bord,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun PrefSwitchRow(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    dark: Boolean,
    onColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val amber = if (dark) AmberDark else AmberLight
    val trOff = if (dark) DarkCardBorder else Color(0xFFE2E8F0)
    val thmOff = if (dark) DarkTextSecondary else LightTextMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, color = onColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (dark) DarkBg else Color.White,
                checkedTrackColor = amber,
                uncheckedThumbColor = thmOff,
                uncheckedTrackColor = trOff
            )
        )
    }
}

@Composable
fun PrefDropdownRow(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    expanded: Boolean,
    onColor: Color,
    modifier: Modifier = Modifier
) {
    val amber = if (isDark()) AmberDark else AmberLight
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, color = onColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                null,
                tint = if (isDark()) DarkTextSecondary else LightTextSecondary
            )
        }
    }
}

@Composable
fun AboutInfoRow(label: String, value: String, on: Color, sub: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = sub, fontSize = 14.sp)
        Text(value, color = on, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Backward-compat stubs (unchanged)
@Composable fun PremiumPrefCard(content: @Composable ColumnScope.() -> Unit) {
    val dark = isDark()
    PrefCard(if (dark) DarkCard else LightCard, if (dark) DarkCardBorder else LightCardBorder, content)
}
@Composable fun PrefDivider() {
    val dark = isDark()
    PrefDividerLine(if (dark) DarkCardBorder else LightCardBorder)
}
@Composable fun PrefSwitchRow(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val dark = isDark()
    val amber = if (dark) AmberDark else AmberLight
    val on = if (dark) DarkTextPrimary else LightTextPrimary
    PrefSwitchRow(label, icon, amber, checked, dark, on, onCheckedChange)
}
@Composable fun PrefDropdownRow(label: String, icon: ImageVector, value: String, expanded: Boolean, modifier: Modifier = Modifier) {
    val dark = isDark()
    val amber = if (dark) AmberDark else AmberLight
    val on = if (dark) DarkTextPrimary else LightTextPrimary
    PrefDropdownRow(label, icon, amber, value, expanded, on, modifier)
}