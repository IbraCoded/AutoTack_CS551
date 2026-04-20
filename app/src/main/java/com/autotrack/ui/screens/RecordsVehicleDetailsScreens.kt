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
fun RecordsScreen(navController: NavController, vm: MainViewModel = hiltViewModel()) {
    val dark     = isDark()
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val records  by vm.allRecords.collectAsStateWithLifecycle()
    val grouped  = vehicles.map { v -> v to records.filter { it.vehicleId == v.id } }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions  = listOf("All") + SERVICE_TYPES

    val bgPage  = if (dark) DarkBg    else LightBg
    val amber   = if (dark) AmberDark else AmberLight
    val on      = if (dark) DarkTextPrimary else LightTextPrimary
    val sub     = if (dark) DarkTextSecondary else LightTextSecondary
    val muted   = if (dark) DarkTextMuted    else LightTextMuted
    val bord    = if (dark) DarkCardBorder   else LightCardBorder

    Scaffold(
        topBar         = { AutoTrackTopBar(title = "RECORDS") },
        bottomBar      = { AutoTrackBottomBar(navController) },
        containerColor = bgPage,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (vehicles.isNotEmpty()) {
                FloatingActionButton(
                    onClick        = { navController.navigate(Screen.AddEditRecord.createRoute(vehicles.first().id)) },
                    containerColor = amber,
                    contentColor   = if (dark) DarkBg else Color.White
                ) { Icon(Icons.Filled.Add, "Add Record") }
            }
        }
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(Modifier.fillMaxSize().carbonFibreBackground(dark).padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Build, null, tint = amber.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Text("NO RECORDS YET", style = MaterialTheme.typography.labelLarge,
                        color = muted, letterSpacing = 2.sp)
                    Text("Add a vehicle then tap + to log a service",
                        style = MaterialTheme.typography.bodySmall, color = muted.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().carbonFibreBackground(dark).padding(padding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter chips
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        filterOptions.forEach { filter ->
                            val selected = selectedFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick  = { selectedFilter = filter },
                                label    = { Text(filter, fontSize = 12.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = amber.copy(alpha = 0.15f),
                                    selectedLabelColor     = amber,
                                    containerColor         = if (dark) DarkCard else Color(0xFFF1F5F9),
                                    labelColor             = sub
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = selected,
                                    borderColor         = bord,
                                    selectedBorderColor = amber
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                grouped.forEach { (vehicle, vehicleRecords) ->
                    val totalSpend = vehicleRecords.sumOf { it.cost }

                    // Vehicle section header
                    item {
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column {
                                Text("${vehicle.make} ${vehicle.model}".uppercase(),
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                    letterSpacing = 0.8.sp, color = on)
                                Text("Total: ${currencyFormat.format(totalSpend)}  ·  ${vehicleRecords.size} records",
                                    color = amber, fontSize = 11.sp)
                            }
                            IconButton(onClick = {
                                navController.navigate(Screen.AddEditRecord.createRoute(vehicle.id))
                            }) { Icon(Icons.Filled.Add, "Add", tint = amber) }
                        }
                        HorizontalDivider(color = bord, thickness = 0.5.dp)
                        Spacer(Modifier.height(4.dp))
                    }

                    val displayRecords = if (selectedFilter == "All") vehicleRecords
                    else vehicleRecords.filter { it.serviceType == selectedFilter }

                    if (displayRecords.isEmpty()) {
                        item {
                            Text(
                                if (selectedFilter == "All") "No records yet — tap + to log a service"
                                else "No $selectedFilter records found",
                                color    = muted,
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
                                            if (result == SnackbarResult.ActionPerformed) deleted = false
                                            else vm.deleteRecord(record)
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
                                                (if (dark) RedDark else RedAccent).copy(alpha = 0.85f)
                                            else Color.Transparent, label = "swipe_bg"
                                        )
                                        Box(Modifier.fillMaxSize()
                                            .background(color, RoundedCornerShape(12.dp))
                                            .padding(end = 20.dp), Alignment.CenterEnd) {
                                            Icon(Icons.Filled.Delete, "Delete",
                                                tint = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    },
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = true
                                ) {
                                    RecordListItem(record, dark) {
                                        navController.navigate(Screen.AddEditRecord.createRoute(vehicle.id, record.id))
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordListItem(record: ServiceRecord, dark: Boolean, onClick: () -> Unit) {
    val amber = if (dark) AmberDark else AmberLight
    val bg    = if (dark) DarkCard  else LightCard
    val bord  = if (dark) DarkCardBorder else LightCardBorder
    val on    = if (dark) DarkTextPrimary else LightTextPrimary
    val sub   = if (dark) DarkTextSecondary else LightTextSecondary
    val iconBg = if (dark) DarkSurface else Color(0xFFF1F5F9)

    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(bg).border(1.dp, bord, RoundedCornerShape(12.dp)).clickable { onClick() }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(iconBg, RoundedCornerShape(8.dp)),
                Alignment.Center) {
                Icon(Icons.Filled.Build, null, tint = amber, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.serviceType, fontWeight = FontWeight.SemiBold, color = on, fontSize = 14.sp)
                Text("${dateFormat.format(Date(record.date))}  ·  ${record.mileage} mi",
                    fontSize = 12.sp, color = sub)
                if (record.garage.isNotBlank()) {
                    Text(record.garage, fontSize = 12.sp, color = sub)
                }
            }
            Text(currencyFormat.format(record.cost), fontWeight = FontWeight.Bold,
                color = amber, fontSize = 14.sp)
        }
    }
}

// ─── Vehicle Detail Screen ────────────────────────────────────────────
@Composable
fun VehicleDetailScreen(navController: NavController, vehicleId: Long, vm: MainViewModel = hiltViewModel()) {
    val dark        = isDark()
    val vehicles    by vm.vehicles.collectAsStateWithLifecycle()
    val vehicle     = vehicles.find { it.id == vehicleId }
    val records     by vm.recordsForVehicle(vehicleId).collectAsStateWithLifecycle(emptyList())
    val avgMpg      by vm.avgMpg(vehicleId).collectAsStateWithLifecycle(null)
    val totalSpend  by vm.totalSpend(vehicleId).collectAsStateWithLifecycle(null)
    val predictions by vm.servicePredictions.collectAsStateWithLifecycle()
    val nextService  = predictions.filter { it.vehicle.id == vehicleId }.minByOrNull { it.daysUntilDue }
    val score        by vm.getHealthScoreFlow(vehicleId).collectAsStateWithLifecycle(100)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    val bgPage  = if (dark) DarkBg    else LightBg
    val amber   = if (dark) AmberDark else AmberLight
    val on      = if (dark) DarkTextPrimary else LightTextPrimary
    val sub     = if (dark) DarkTextSecondary else LightTextSecondary
    val bg      = if (dark) DarkCard  else LightCard
    val bord    = if (dark) DarkCardBorder else LightCardBorder

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title    = vehicle?.let { "${it.make} ${it.model}".uppercase() } ?: "VEHICLE",
                showBack = true,
                onBack   = { navController.popBackStack() }
            )
        },
        containerColor = bgPage,
        snackbarHost   = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        vehicle?.let { v ->
            LazyColumn(
                modifier            = Modifier.fillMaxSize().carbonFibreBackground(dark).padding(padding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Vehicle stats card
                item {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(bg).border(1.dp, bord, RoundedCornerShape(14.dp))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            HealthScoreRing(score = score, size = 80f)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("${v.year}  ·  ${v.colour}", color = sub, fontSize = 13.sp)
                                Text("${v.mileage} mi", fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp, color = on)
                                Text(v.fuelType, color = amber, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Next service prediction
                nextService?.let { pred ->
                    item {
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(bg).border(1.dp, amber.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(16.dp)) {
                            Column {
                                Text("NEXT SERVICE", fontSize = 9.sp, letterSpacing = 2.sp, color = amber)
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text(pred.serviceType, fontWeight = FontWeight.SemiBold, color = on)
                                    UrgencyBadge(pred.daysUntilDue)
                                }
                                Text("~${dateFormat.format(Date(pred.predictedDueDate))}  ·  ${pred.predictedDueMileage} mi",
                                    fontSize = 13.sp, color = sub)
                            }
                        }
                    }
                }

                // Stat row
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailStatCard("Records",    "${records.size}",
                            amber, on, sub, bg, bord, Modifier.weight(1f))
                        DetailStatCard("Total Spend",
                            totalSpend?.let { currencyFormat.format(it) } ?: "£0",
                            amber, on, sub, bg, bord, Modifier.weight(1f))
                        DetailStatCard("Avg MPG",
                            avgMpg?.toDouble()?.let { "%.1f".format(it) } ?: "—",
                            amber, on, sub, bg, bord, Modifier.weight(1f))
                    }
                }

                // Action buttons
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { navController.navigate(Screen.AddEditRecord.createRoute(vehicleId)) },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = amber),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, amber.copy(alpha = 0.5f))
                        ) { Text("Add Record") }
                        OutlinedButton(onClick = { navController.navigate(Screen.AddEditFuel.createRoute(vehicleId)) },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = amber),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, amber.copy(alpha = 0.5f))
                        ) { Text("Add Fuel") }
                    }
                }

                item {
                    Text("SERVICE HISTORY", fontSize = 10.sp, letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold, color = amber)
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
                                        "${record.serviceType} deleted", "UNDO", duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) deleted = false
                                    else vm.deleteRecord(record)
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
                                        (if (dark) RedDark else RedAccent).copy(alpha = 0.85f)
                                    else Color.Transparent, label = "swipe_bg"
                                )
                                Box(Modifier.fillMaxSize().background(color, RoundedCornerShape(12.dp))
                                    .padding(end = 20.dp), Alignment.CenterEnd) {
                                    Icon(Icons.Filled.Delete, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true
                        ) {
                            RecordListItem(record, dark) {
                                navController.navigate(Screen.AddEditRecord.createRoute(vehicleId, record.id))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        } ?: LoadingBox()
    }
}

@Composable
fun DetailStatCard(
    label: String, value: String, amber: Color, on: Color, sub: Color,
    bg: Color, bord: Color, modifier: Modifier = Modifier
) {
    Box(modifier.background(bg, RoundedCornerShape(10.dp))
        .border(1.dp, bord, RoundedCornerShape(10.dp)).padding(12.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = on)
            Text(label, fontSize = 10.sp, color = amber, letterSpacing = 0.5.sp)
        }
    }
}

// Keep old StatCard name for any other references
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val dark  = isDark()
    val amber = if (dark) AmberDark else AmberLight
    val on    = if (dark) DarkTextPrimary else LightTextPrimary
    val bg    = if (dark) DarkCard  else LightCard
    val bord  = if (dark) DarkCardBorder else LightCardBorder
    DetailStatCard(label, value, amber, on, on, bg, bord, modifier)
}
