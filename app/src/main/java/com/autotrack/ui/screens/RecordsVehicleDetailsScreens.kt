package com.autotrack.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.autotrack.data.local.entity.ServiceRecord
import com.autotrack.navigation.Screen
import com.autotrack.ui.components.*
import com.autotrack.ui.theme.*
import com.autotrack.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val dateFormat     = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    navController: NavController,
    vm: MainViewModel = hiltViewModel()
) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val records  by vm.allRecords.collectAsStateWithLifecycle()
    val grouped  = vehicles.map { v -> v to records.filter { it.vehicleId == v.id } }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions  = listOf("All") + SERVICE_TYPES

    Scaffold(
        topBar         = { AutoTrackTopBar(title = "RECORDS") },
        bottomBar      = { AutoTrackBottomBar(navController) },
        containerColor = Obsidian,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (vehicles.isNotEmpty()) {
                FloatingActionButton(
                    onClick        = { navController.navigate(Screen.AddEditRecord.createRoute(vehicles.first().id)) },
                    containerColor = GoldPrimary,
                    contentColor   = Obsidian
                ) { Icon(Icons.Filled.Add, contentDescription = "Add Record") }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().background(Obsidian).padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { filter ->
                        val selected = selectedFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick  = { selectedFilter = filter },
                            label    = { Text(filter, fontSize = 12.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary.copy(alpha = 0.15f),
                                selectedLabelColor     = GoldPrimary,
                                containerColor         = GunmetalMid,
                                labelColor             = SilverMid
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled             = true,
                                selected            = selected,
                                borderColor         = GunmetalLight,
                                selectedBorderColor = GoldPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            grouped.forEach { (vehicle, vehicleRecords) ->
                val totalSpend = vehicleRecords.sumOf { it.cost }

                item {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${vehicle.make} ${vehicle.model}".uppercase(),
                                fontWeight    = FontWeight.Bold,
                                fontSize      = 13.sp,
                                letterSpacing = 0.8.sp,
                                color         = ChromeWhite
                            )
                            Text(
                                "Total: ${currencyFormat.format(totalSpend)}  ·  ${vehicleRecords.size} records",
                                color    = GoldPrimary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = {
                            navController.navigate(Screen.AddEditRecord.createRoute(vehicle.id))
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = GoldPrimary)
                        }
                    }
                    HorizontalDivider(color = GunmetalLight, thickness = 0.5.dp)
                    Spacer(Modifier.height(4.dp))
                }

                val displayRecords = if (selectedFilter == "All") vehicleRecords
                else vehicleRecords.filter { it.serviceType == selectedFilter }

                if (displayRecords.isEmpty()) {
                    item {
                        Text(
                            if (selectedFilter == "All") "No records yet — tap + to log a service"
                            else "No $selectedFilter records found",
                            color    = SilverDim,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                } else {
                    items(displayRecords, key = { it.id }) { record ->
                        var deleted by remember { mutableStateOf(false) }
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart && !deleted) {
                                    deleted = true
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message     = "${record.serviceType} record deleted",
                                            actionLabel = "UNDO",
                                            duration    = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            deleted = false
                                        } else {
                                            vm.deleteRecord(record)
                                        }
                                    }
                                    true
                                } else false
                            }
                        )
                        if (!deleted) {
                            SwipeToDismissBox(
                                state             = dismissState,
                                backgroundContent = {
                                    val color by animateColorAsState(
                                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                            CrimsonAlert.copy(alpha = 0.85f)
                                        else Color.Transparent,
                                        label = "swipe_bg"
                                    )
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(color, RoundedCornerShape(12.dp))
                                            .padding(end = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            tint     = ChromeWhite,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true
                            ) {
                                RecordListItem(
                                    record  = record,
                                    onClick = {
                                        navController.navigate(
                                            Screen.AddEditRecord.createRoute(vehicle.id, record.id)
                                        )
                                    }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordListItem(record: ServiceRecord, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GunmetalDeep)
            .border(1.dp, GunmetalLight, RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier.size(36.dp).background(GunmetalMid, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Build, null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.serviceType, fontWeight = FontWeight.SemiBold, color = ChromeWhite, fontSize = 14.sp)
                Text(
                    "${dateFormat.format(Date(record.date))}  ·  ${record.mileage} mi",
                    fontSize = 12.sp, color = SilverDim
                )
                if (record.garage.isNotBlank()) {
                    Text(record.garage, fontSize = 12.sp, color = SilverMid)
                }
            }
            Text(currencyFormat.format(record.cost), fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 14.sp)
        }
    }
}

// ─── Vehicle Detail Screen ────────────────────────────────────
@Composable
fun VehicleDetailScreen(
    navController: NavController,
    vehicleId: Long,
    vm: MainViewModel = hiltViewModel()
) {
    val vehicles    by vm.vehicles.collectAsStateWithLifecycle()
    val vehicle     = vehicles.find { it.id == vehicleId }
    val records     by vm.recordsForVehicle(vehicleId).collectAsStateWithLifecycle(emptyList())
    val avgMpg      by vm.avgMpg(vehicleId).collectAsStateWithLifecycle(null)
    val totalSpend  by vm.totalSpend(vehicleId).collectAsStateWithLifecycle(null)
    val predictions by vm.servicePredictions.collectAsStateWithLifecycle()
    val nextService  = predictions.filter { it.vehicle.id == vehicleId }.minByOrNull { it.daysUntilDue }
    val score        = vm.healthScore(vehicleId)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title    = vehicle?.let { "${it.make} ${it.model}".uppercase() } ?: "VEHICLE",
                showBack = true,
                onBack   = { navController.popBackStack() }
            )
        },
        containerColor = Obsidian,
        snackbarHost   = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        vehicle?.let { v ->
            LazyColumn(
                modifier            = Modifier.fillMaxSize().background(Obsidian).padding(padding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GunmetalDeep, RoundedCornerShape(14.dp))
                            .border(1.dp, GunmetalLight, RoundedCornerShape(14.dp))
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            HealthScoreRing(score = score, size = 80f)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("${v.year}  ·  ${v.colour}", color = SilverMid, fontSize = 13.sp)
                                Text("${v.mileage} mi", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ChromeWhite)
                                Text(v.fuelType, color = GoldPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }

                nextService?.let { pred ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GunmetalDeep, RoundedCornerShape(14.dp))
                                .border(1.dp, GoldDim.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text("NEXT SERVICE", fontSize = 9.sp, letterSpacing = 2.sp, color = GoldPrimary)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(pred.serviceType, fontWeight = FontWeight.SemiBold, color = ChromeWhite)
                                    UrgencyBadge(pred.daysUntilDue)
                                }
                                Text(
                                    "~${dateFormat.format(Date(pred.predictedDueDate))}  ·  ${pred.predictedDueMileage} mi",
                                    fontSize = 13.sp, color = SilverMid
                                )
                            }
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Records", "${records.size}", Modifier.weight(1f))
                        StatCard("Total Spend", totalSpend?.let { currencyFormat.format(it) } ?: "£0", Modifier.weight(1f))
                        StatCard("Avg MPG", avgMpg?.let { "%.1f".format(it) } ?: "-", Modifier.weight(1f))
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick  = { navController.navigate(Screen.AddEditRecord.createRoute(vehicleId)) },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, GoldDim)
                        ) { Text("Add Record") }
                        OutlinedButton(
                            onClick  = { navController.navigate(Screen.AddEditFuel.createRoute(vehicleId)) },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, GoldDim)
                        ) { Text("Add Fuel") }
                    }
                }

                item {
                    Text("SERVICE HISTORY", fontSize = 10.sp, letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold, color = GoldPrimary)
                }

                @OptIn(ExperimentalMaterial3Api::class)
                items(records, key = { it.id }) { record ->
                    var deleted by remember { mutableStateOf(false) }
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart && !deleted) {
                                deleted = true
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message     = "${record.serviceType} deleted",
                                        actionLabel = "UNDO",
                                        duration    = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        deleted = false
                                    } else {
                                        vm.deleteRecord(record)
                                    }
                                }
                                true
                            } else false
                        }
                    )
                    if (!deleted) {
                        SwipeToDismissBox(
                            state             = dismissState,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                        CrimsonAlert.copy(alpha = 0.85f) else Color.Transparent,
                                    label = "swipe_bg"
                                )
                                Box(
                                    Modifier.fillMaxSize().background(color, RoundedCornerShape(12.dp)).padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Filled.Delete, null, tint = ChromeWhite, modifier = Modifier.size(22.dp))
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true
                        ) {
                            RecordListItem(
                                record  = record,
                                onClick = { navController.navigate(Screen.AddEditRecord.createRoute(vehicleId, record.id)) }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        } ?: LoadingBox()
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(GunmetalDeep, RoundedCornerShape(10.dp))
            .border(1.dp, GunmetalLight, RoundedCornerShape(10.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ChromeWhite)
            Text(label, fontSize = 10.sp, color = GoldPrimary, letterSpacing = 0.5.sp)
        }
    }
}