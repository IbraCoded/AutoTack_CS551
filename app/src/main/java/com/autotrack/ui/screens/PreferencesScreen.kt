package com.autotrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.autotrack.ui.components.AutoTrackTopBar
import com.autotrack.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    navController: NavController,
    vm: MainViewModel = hiltViewModel()
) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()

    var intervalExpanded by remember { mutableStateOf(false) }
    var distanceExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var thresholdText by remember(prefs) {
        mutableStateOf(prefs.overdueThresholdDays.toString())
    }

    val intervals = listOf("Daily", "Weekly", "Fortnightly", "Monthly")
    val distUnits = listOf("mi", "km")
    val currencies = listOf("GBP £", "USD $", "EUR €")

    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title = "Preferences",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // APPEARANCE
            item {
                Text(
                    "APPEARANCE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            item {
                Card {
                    Column(Modifier.padding(4.dp)) {

                        PrefSwitchRow(
                            label = "Dark Theme",
                            icon = Icons.Filled.DarkMode,
                            checked = prefs.darkTheme,
                            onCheckedChange = { vm.setDarkTheme(it) }
                        )

                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                        // Distance unit
                        ExposedDropdownMenuBox(
                            expanded = distanceExpanded,
                            onExpandedChange = { distanceExpanded = it }
                        ) {
                            PrefDropdownRow(
                                label = "Distance Unit",
                                icon = Icons.Filled.Speed,
                                value = prefs.distanceUnit,
                                expanded = distanceExpanded,
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = distanceExpanded,
                                onDismissRequest = { distanceExpanded = false }
                            ) {
                                distUnits.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u) },
                                        onClick = {
                                            vm.setDistanceUnit(u)
                                            distanceExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                        // Currency
                        ExposedDropdownMenuBox(
                            expanded = currencyExpanded,
                            onExpandedChange = { currencyExpanded = it }
                        ) {
                            PrefDropdownRow(
                                label = "Currency",
                                icon = Icons.Filled.AttachMoney,
                                value = prefs.currency,
                                expanded = currencyExpanded,
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = currencyExpanded,
                                onDismissRequest = { currencyExpanded = false }
                            ) {
                                currencies.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c) },
                                        onClick = {
                                            vm.setCurrency(c)
                                            currencyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            //NOTIFICATIONS
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text(
                    "NOTIFICATIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            item {
                Card {
                    Column(Modifier.padding(4.dp)) {

                        PrefSwitchRow(
                            label = "Service Reminders",
                            icon = Icons.Filled.Notifications,
                            checked = prefs.remindersEnabled,
                            onCheckedChange = { vm.setRemindersEnabled(it) }
                        )

                        if (prefs.remindersEnabled) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            ExposedDropdownMenuBox(
                                expanded = intervalExpanded,
                                onExpandedChange = { intervalExpanded = it }
                            ) {
                                PrefDropdownRow(
                                    label = "Reminder Interval",
                                    icon = Icons.Filled.Schedule,
                                    value = prefs.reminderInterval,
                                    expanded = intervalExpanded,
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = intervalExpanded,
                                    onDismissRequest = { intervalExpanded = false }
                                ) {
                                    intervals.forEach { i ->
                                        DropdownMenuItem(
                                            text = { Text(i) },
                                            onClick = {
                                                vm.setReminderInterval(i)
                                                intervalExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                        PrefSwitchRow(
                            label = "Mileage Alerts",
                            icon = Icons.Filled.Speed,
                            checked = prefs.mileageAlertsEnabled,
                            onCheckedChange = { vm.setMileageAlerts(it) }
                        )

                        if (prefs.mileageAlertsEnabled) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Overdue threshold (days)")
                                OutlinedTextField(
                                    value = thresholdText,
                                    onValueChange = {
                                        thresholdText = it
                                        it.toIntOrNull()?.let { d ->
                                            vm.setOverdueThreshold(d)
                                        }
                                    },
                                    modifier = Modifier.width(80.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Preference row components
@Composable
fun PrefSwitchRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(label)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PrefDropdownRow(
    label: String,
    icon: ImageVector,
    value: String,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(label)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = MaterialTheme.colorScheme.primary)
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}