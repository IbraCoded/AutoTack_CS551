package com.autotrack.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.autotrack.data.local.entity.ServiceRecord
import com.autotrack.navigation.Screen
import com.autotrack.ui.components.AutoTrackBottomBar
import com.autotrack.ui.components.AutoTrackTopBar
import com.autotrack.ui.components.HealthScoreRing
import com.autotrack.ui.components.LoadingBox
import com.autotrack.ui.components.UrgencyBadge
import com.autotrack.ui.theme.RedAlert
import com.autotrack.viewmodel.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Fix local date issue
private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK)

// RECORDS SCREEN
@Composable
fun RecordsScreen(
    navController: NavController,
    vm: MainViewModel = hiltViewModel()
) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val records by vm.allRecords.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title = "Records",
                showSettings = false
            )
        },
        bottomBar = { AutoTrackBottomBar(navController) },
        floatingActionButton = {
            if (vehicles.isNotEmpty()) {
                FloatingActionButton(onClick = {
                    navController.navigate(
                        Screen.AddEditRecord.createRoute(vehicles.first().id)
                    )
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Record")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            vehicles.forEach { vehicle ->
                val vehicleRecords = records.filter { it.vehicleId == vehicle.id }
                val totalSpend = vehicleRecords.sumOf { it.cost }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${vehicle.make} ${vehicle.model}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "Total: ${currencyFormat.format(totalSpend)}" +
                                        " | ${vehicleRecords.size} records",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 12.sp
                            )
                        }
                        Row {
                            // Share service report
                            IconButton(onClick = {
                                shareServiceReport(
                                    context, vehicle.make,
                                    vehicle.model, vehicleRecords
                                )
                            }) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = "Share Report",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            // Add record for this vehicle
                            IconButton(onClick = {
                                navController.navigate(
                                    Screen.AddEditRecord.createRoute(vehicle.id)
                                )
                            }) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Add Record"
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }

                if (vehicleRecords.isEmpty()) {
                    item {
                        Text(
                            "No records yet",
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(
                                vertical = 8.dp, horizontal = 4.dp
                            )
                        )
                    }
                } else {
                    items(vehicleRecords, key = { it.id }) { record ->
                        RecordListItem(
                            record = record,
                            onClick = {
                                navController.navigate(
                                    Screen.AddEditRecord.createRoute(
                                        vehicle.id, record.id
                                    )
                                )
                            },
                            onDelete = { vm.deleteRecord(record) }
                        )
                    }
                }
            }
        }
    }
}

// Share service report
fun shareServiceReport(
    context: android.content.Context,
    make: String,
    model: String,
    records: List<ServiceRecord>
) {
    val sb = StringBuilder()
    sb.appendLine("Service Report — $make $model")
    sb.appendLine("Generated: ${dateFormat.format(Date())}")
    sb.appendLine("─────────────────────────")
    records.sortedByDescending { it.date }.forEach { r ->
        sb.appendLine("${r.serviceType}  |  ${dateFormat.format(Date(r.date))}")
        sb.appendLine("  Mileage: ${r.mileage} mi  |  Cost: ${currencyFormat.format(r.cost)}")
        if (r.garage.isNotBlank()) sb.appendLine("  Garage: ${r.garage}")
        if (r.notes.isNotBlank()) sb.appendLine("  Notes: ${r.notes}")
        sb.appendLine()
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Service Report — $make $model")
        putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(
        android.content.Intent.createChooser(intent, "Share Service Report")
    )
}

// Record list item
@Composable
fun RecordListItem(
    record: ServiceRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete Record") },
            text = { Text("Delete this ${record.serviceType} record?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDelete = false }) {
                    Text("Delete", color = RedAlert)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.serviceType, fontWeight = FontWeight.SemiBold)
                Text(
                    "${dateFormat.format(Date(record.date))}  ·  ${record.mileage} mi",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                if (record.garage.isNotBlank()) {
                    Text(
                        record.garage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Text(
                currencyFormat.format(record.cost),
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showDelete = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}


// VEHICLE DETAIL SCREEN
@Composable
fun VehicleDetailScreen(
    navController: NavController,
    vehicleId: Long,
    vm: MainViewModel = hiltViewModel()
) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val vehicle = vehicles.find { it.id == vehicleId }
    val records by vm.recordsForVehicle(vehicleId)
        .collectAsStateWithLifecycle(emptyList())
    val fuelCount by vm.fuelCount(vehicleId)
        .collectAsStateWithLifecycle(0)
    val avgMpg by vm.avgMpg(vehicleId)
        .collectAsStateWithLifecycle(null)
    val totalSpend by vm.totalSpend(vehicleId)
        .collectAsStateWithLifecycle(null)
    val predictions by vm.servicePredictions.collectAsStateWithLifecycle()
    val nextService = predictions
        .filter { it.vehicle.id == vehicleId }
        .minByOrNull { it.daysUntilDue }
    val score = if (vehicle != null) vm.healthScore(vehicleId) else 0

    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title = vehicle?.let { "${it.make} ${it.model}" } ?: "Vehicle",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        if (vehicle == null) {
            LoadingBox()
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Vehicle summary card
            item {
                Card {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HealthScoreRing(score = score, size = 80f)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "${vehicle.year} · ${vehicle.colour}",
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "${vehicle.mileage} mi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                vehicle.fuelType,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Next service prediction
            nextService?.let { pred ->
                item {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "NEXT SERVICE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    pred.serviceType,
                                    fontWeight = FontWeight.SemiBold
                                )
                                UrgencyBadge(pred.daysUntilDue)
                            }
                            Text(
                                "~${dateFormat.format(Date(pred.predictedDueDate))}" +
                                        "  ·  ${pred.predictedDueMileage} mi",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "[${pred.confidence} confidence]",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Stats cards row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Records",
                        value = "${records.size}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Spent",
                        value = totalSpend?.let {
                            currencyFormat.format(it)
                        } ?: "£0",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Avg MPG",
                        value = avgMpg?.let {
                            "%.1f".format(it)
                        } ?: "-",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            navController.navigate(
                                Screen.AddEditRecord.createRoute(vehicleId)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Add Record") }

                    OutlinedButton(
                        onClick = {
                            navController.navigate(
                                Screen.AddEditFuel.createRoute(vehicleId)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Add Fuel") }
                }
            }

            // Service history
            item {
                Text(
                    "SERVICE HISTORY",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            items(records, key = { it.id }) { record ->
                RecordListItem(
                    record = record,
                    onClick = {
                        navController.navigate(
                            Screen.AddEditRecord.createRoute(vehicleId, record.id)
                        )
                    },
                    onDelete = { vm.deleteRecord(record) }
                )
            }
        }
    }
}

// Stats card
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}