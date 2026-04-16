package com.autotrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.autotrack.data.local.entity.FuelEntry
import com.autotrack.data.local.entity.ServiceRecord
import com.autotrack.data.local.entity.Vehicle
import com.autotrack.navigation.Screen
import com.autotrack.ui.components.AutoTrackBottomBar
import com.autotrack.ui.components.AutoTrackTopBar
import com.autotrack.ui.components.UrgencyBadge
import com.autotrack.ui.theme.AmberWarn
import com.autotrack.ui.theme.GreenSuccess
import com.autotrack.ui.theme.RedAlert
import com.autotrack.viewmodel.MainViewModel
import com.autotrack.viewmodel.ServicePrediction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
private val ccyFmt  = NumberFormat.getCurrencyInstance(Locale.UK)


// FUEL SCREEN

@Composable
fun FuelScreen(
    navController: NavController,
    vm: MainViewModel = hiltViewModel()
) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()

    Scaffold(
        topBar    = { AutoTrackTopBar(title = "Fuel Log") },
        bottomBar = { AutoTrackBottomBar(navController) },
        floatingActionButton = {
            if (vehicles.isNotEmpty()) {
                FloatingActionButton(onClick = {
                    navController.navigate(
                        Screen.AddEditFuel.createRoute(vehicles.first().id)
                    )
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Log Fuel")
                }
            }
        }
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Add a vehicle to start logging fuel",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier            = Modifier.padding(padding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vehicles.forEach { vehicle ->
                    item {
                        VehicleFuelSection(
                            vehicle       = vehicle,
                            navController = navController,
                            vm            = vm
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleFuelSection(
    vehicle: Vehicle,
    navController: NavController,
    vm: MainViewModel
) {
    val avgMpg  by vm.avgMpg(vehicle.id).collectAsStateWithLifecycle(null)
    val entries by vm.fuelForVehicle(vehicle.id).collectAsStateWithLifecycle(emptyList())

    Column {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "${vehicle.make} ${vehicle.model}",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp
                )
                Text(
                    avgMpg?.let { "Avg: ${"%.1f".format(it)} MPG" } ?: "No fuel data",
                    color    = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = {
                navController.navigate(
                    Screen.AddEditFuel.createRoute(vehicle.id)
                )
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Fuel")
            }
        }

        HorizontalDivider()
        Spacer(Modifier.height(4.dp))

        if (entries.isEmpty()) {
            Text(
                "No fuel logs yet",
                color    = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            entries.take(10).forEach { entry ->
                FuelEntryItem(
                    entry    = entry,
                    onClick  = {
                        navController.navigate(
                            Screen.AddEditFuel.createRoute(vehicle.id, entry.id)
                        )
                    },
                    onDelete = { vm.deleteFuelEntry(entry) }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun FuelEntryItem(
    entry: FuelEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title            = { Text("Delete Entry") },
            text             = { Text("Delete this fuel log entry?") },
            confirmButton    = {
                TextButton(onClick = { onDelete(); showDelete = false }) {
                    Text("Delete", color = RedAlert)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LocalGasStation,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    dateFmt.format(Date(entry.date)),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${"%.1f".format(entry.litresFilled)}L" +
                            "  ·  ${entry.mileageAtFill} mi",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.outline
                )
                if (entry.mpg > 0) {
                    val mpgColor = when {
                        entry.mpg >= 40 -> GreenSuccess
                        entry.mpg >= 30 -> AmberWarn
                        else            -> RedAlert
                    }
                    Text(
                        "${"%.1f".format(entry.mpg)} MPG",
                        color      = mpgColor,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                ccyFmt.format(entry.totalCost),
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showDelete = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp),
                    tint               = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}


// SERVICES SCREEN

@Composable
fun ServicesScreen(
    navController: NavController,
    vm: MainViewModel = hiltViewModel()
) {
    val predictions by vm.servicePredictions.collectAsStateWithLifecycle()

    Scaffold(
        topBar    = { AutoTrackTopBar(title = "Upcoming Services") },
        bottomBar = { AutoTrackBottomBar(navController) }
    ) { padding ->
        if (predictions.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Add vehicles to see service predictions",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier            = Modifier.padding(padding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Smart Service Prediction",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                }

                items(predictions) { pred ->
                    ServicePredictionCard(
                        pred           = pred,
                        onMarkComplete = {
                            vm.insertRecord(
                                ServiceRecord(
                                    vehicleId   = pred.vehicle.id,
                                    serviceType = pred.serviceType,
                                    date        = System.currentTimeMillis(),
                                    mileage     = pred.vehicle.mileage,
                                    cost        = 0.0
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ServicePredictionCard(
    pred: ServicePrediction,
    onMarkComplete: () -> Unit
) {
    val urgencyColor = when {
        pred.isOverdue          -> RedAlert
        pred.daysUntilDue <= 7  -> RedAlert
        pred.daysUntilDue <= 30 -> AmberWarn
        else                    -> GreenSuccess
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = urgencyColor.copy(alpha = 0.05f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${pred.vehicle.make} ${pred.vehicle.model}" +
                            " — ${pred.serviceType}",
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f)
                )
                UrgencyBadge(pred.daysUntilDue)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Due: ${dateFmt.format(Date(pred.predictedDueDate))}" +
                        "  ·  ~${pred.predictedDueMileage} mi",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.outline
            )
            Text(
                "[${pred.confidence} confidence]",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick  = onMarkComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier           = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Mark Complete")
            }
        }
    }
}


// ANALYTICS SCREEN
@Composable
fun AnalyticsScreen(
    navController: NavController,
    vm: MainViewModel = hiltViewModel()
) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val records  by vm.allRecords.collectAsStateWithLifecycle()
    var selectedVehicleId by remember { mutableStateOf<Long?>(null) }
    var expanded          by remember { mutableStateOf(false) }

    val filteredRecords = if (selectedVehicleId != null)
        records.filter { it.vehicleId == selectedVehicleId }
    else records

    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }
    val costByType      = filteredRecords
        .groupBy { it.serviceType }
        .mapValues { (_, v) -> v.sumOf { it.cost } }
        .entries
        .sortedByDescending { it.value }
    val totalSpend = filteredRecords.sumOf { it.cost }

    Scaffold(
        topBar    = { AutoTrackTopBar(title = "Analytics") },
        bottomBar = { AutoTrackBottomBar(navController) }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle filter dropdown
            item {
                Box {
                    OutlinedButton(
                        onClick  = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            selectedVehicle?.let {
                                "${it.make} ${it.model}"
                            } ?: "All Vehicles"
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded         = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text    = { Text("All Vehicles") },
                            onClick = { selectedVehicleId = null; expanded = false }
                        )
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text    = { Text("${v.make} ${v.model}") },
                                onClick = { selectedVehicleId = v.id; expanded = false }
                            )
                        }
                    }
                }
            }

            // Total spend card
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "TOTAL SPEND",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            ccyFmt.format(totalSpend),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 28.sp,
                            color      = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "across ${filteredRecords.size} service records",
                            color    = MaterialTheme.colorScheme.outline,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Cost by service type
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "COST BY SERVICE TYPE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))

                        if (costByType.isEmpty()) {
                            Text(
                                "No data yet",
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            val maxCost = costByType.maxOf { it.value }
                            costByType.forEach { (type, cost) ->
                                Column(Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(type, fontSize = 13.sp)
                                        Text(
                                            ccyFmt.format(cost),
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = 13.sp
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = { (cost / maxCost).toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color    = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // MPG summary
            if (vehicles.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "MPG SUMMARY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(8.dp))
                            MpgSummaryList(
                                vehicles = if (selectedVehicleId != null)
                                    vehicles.filter { it.id == selectedVehicleId }
                                else vehicles,
                                vm = vm
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MpgSummaryList(
    vehicles: List<Vehicle>,
    vm: MainViewModel
) {
    vehicles.forEach { v ->
        val avgMpg by vm.avgMpg(v.id).collectAsStateWithLifecycle(null)
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${v.make} ${v.model}", fontSize = 13.sp)
            Text(
                avgMpg?.let { "${"%.1f".format(it)} MPG" } ?: "—",
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
        }
    }
}