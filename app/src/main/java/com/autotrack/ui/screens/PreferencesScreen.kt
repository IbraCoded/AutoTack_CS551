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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.autotrack.ui.components.AutoTrackTopBar
import com.autotrack.ui.theme.*
import com.autotrack.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    navController: NavController,
    vm: MainViewModel = hiltViewModel()
) {
    val prefs            by vm.preferences.collectAsStateWithLifecycle()
    var intervalExpanded by remember { mutableStateOf(false) }
    var distanceExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var thresholdText    by remember(prefs) { mutableStateOf(prefs.overdueThresholdDays.toString()) }

    val intervals  = listOf("Daily", "Weekly", "Fortnightly", "Monthly")
    val distUnits  = listOf("mi", "km")
    val currencies = listOf("GBP £", "USD $", "EUR €")

    Scaffold(
        topBar         = { AutoTrackTopBar(title = "PREFERENCES", showBack = true, onBack = { navController.popBackStack() }) },
        containerColor = Obsidian
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().background(Obsidian).padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionLabel("APPEARANCE") }
            item {
                PremiumPrefCard {
                    PrefSwitchRow("Dark Theme", Icons.Filled.DarkMode, prefs.darkTheme) { vm.setDarkTheme(it) }
                    PrefDivider()
                    ExposedDropdownMenuBox(expanded = distanceExpanded, onExpandedChange = { distanceExpanded = it }) {
                        PrefDropdownRow("Distance Unit", Icons.Filled.Speed, prefs.distanceUnit, distanceExpanded, Modifier.menuAnchor())
                        ExposedDropdownMenu(expanded = distanceExpanded, onDismissRequest = { distanceExpanded = false },
                            modifier = Modifier.background(GunmetalMid)) {
                            distUnits.forEach { u ->
                                DropdownMenuItem(text = { Text(u, color = ChromeWhite) },
                                    onClick = { vm.setDistanceUnit(u); distanceExpanded = false })
                            }
                        }
                    }
                    PrefDivider()
                    ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                        PrefDropdownRow("Currency", Icons.Filled.AttachMoney, prefs.currency, currencyExpanded, Modifier.menuAnchor())
                        ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false },
                            modifier = Modifier.background(GunmetalMid)) {
                            currencies.forEach { c ->
                                DropdownMenuItem(text = { Text(c, color = ChromeWhite) },
                                    onClick = { vm.setCurrency(c); currencyExpanded = false })
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }
            item { SectionLabel("NOTIFICATIONS") }
            item {
                PremiumPrefCard {
                    PrefSwitchRow("Service Reminders", Icons.Filled.Notifications, prefs.remindersEnabled) { vm.setRemindersEnabled(it) }
                    if (prefs.remindersEnabled) {
                        PrefDivider()
                        ExposedDropdownMenuBox(expanded = intervalExpanded, onExpandedChange = { intervalExpanded = it }) {
                            PrefDropdownRow("Reminder Interval", Icons.Filled.Schedule, prefs.reminderInterval, intervalExpanded, Modifier.menuAnchor())
                            ExposedDropdownMenu(expanded = intervalExpanded, onDismissRequest = { intervalExpanded = false },
                                modifier = Modifier.background(GunmetalMid)) {
                                intervals.forEach { i ->
                                    DropdownMenuItem(text = { Text(i, color = ChromeWhite) },
                                        onClick = { vm.setReminderInterval(i); intervalExpanded = false })
                                }
                            }
                        }
                    }
                    PrefDivider()
                    PrefSwitchRow("Mileage Alerts", Icons.Filled.Speed, prefs.mileageAlertsEnabled) { vm.setMileageAlerts(it) }
                    if (prefs.mileageAlertsEnabled) {
                        PrefDivider()
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Overdue threshold (days)", color = SilverMid, fontSize = 13.sp)
                            OutlinedTextField(
                                value         = thresholdText,
                                onValueChange = {
                                    thresholdText = it
                                    it.toIntOrNull()?.let { d -> vm.setOverdueThreshold(d) }
                                },
                                modifier   = Modifier.width(80.dp),
                                singleLine = true,
                                colors     = premiumTextFieldColors()
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }
            item { SectionLabel("SENSORS") }
            item {
                PremiumPrefCard {
                    PrefSwitchRow(
                        label           = "Shake to Log Service",
                        icon            = Icons.Filled.Vibration,
                        checked         = prefs.shakeEnabled,
                        onCheckedChange = { vm.setShakeEnabled(it) }
                    )
                    PrefDivider()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "Shake your phone to quickly open the Log Service screen",
                            color    = SilverDim,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumPrefCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GunmetalDeep, RoundedCornerShape(14.dp))
            .border(1.dp, GunmetalLight, RoundedCornerShape(14.dp)),
        content  = content
    )
}

@Composable
fun PrefDivider() {
    HorizontalDivider(color = GunmetalLight, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun PrefSwitchRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, color = ChromeWhite, fontSize = 14.sp)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Obsidian,
                checkedTrackColor   = GoldPrimary,
                uncheckedThumbColor = SilverDim,
                uncheckedTrackColor = GunmetalLight
            )
        )
    }
}

@Composable
fun PrefDropdownRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, color = ChromeWhite, fontSize = 14.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = SilverDim)
        }
    }
}