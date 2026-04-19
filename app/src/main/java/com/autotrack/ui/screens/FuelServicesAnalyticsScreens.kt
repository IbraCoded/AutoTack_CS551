package com.autotrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.autotrack.ui.components.*
import com.autotrack.ui.theme.*
import com.autotrack.viewmodel.MainViewModel
import com.autotrack.viewmodel.ServicePrediction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
private val ccyFmt  = NumberFormat.getCurrencyInstance(Locale.UK)

// ────────────────────────────────────────────────────────────────────────
// FUEL SCREEN
// ────────────────────────────────────────────────────────────────────────
@Composable
fun FuelScreen(navController: NavController, vm: MainViewModel = hiltViewModel()) {
    val dark     = isDark()
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val bgPage   = if (dark) DarkBg    else LightBg
    val amber    = if (dark) AmberDark else AmberLight

    Scaffold(
        topBar         = { AutoTrackTopBar(title = "Fuel Log") },
        bottomBar      = { AutoTrackBottomBar(navController) },
        containerColor = bgPage,
        floatingActionButton = {
            if (vehicles.isNotEmpty()) {
                FloatingActionButton(
                    onClick        = { navController.navigate(Screen.AddEditFuel.createRoute(vehicles.first().id)) },
                    containerColor = amber,
                    contentColor   = if (dark) DarkBg else Color.White
                ) { Icon(Icons.Filled.Add, "Log Fuel") }
            }
        }
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(Modifier.fillMaxSize().carbonFibreBackground(dark).padding(padding),
                contentAlignment = Alignment.Center) {
                Text("Add a vehicle to start logging fuel",
                    color = if (dark) DarkTextMuted else LightTextMuted)
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().carbonFibreBackground(dark).padding(padding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                vehicles.forEach { vehicle ->
                    item { VehicleFuelSection(vehicle, navController, vm, dark) }
                }
            }
        }
    }
}

@Composable
private fun VehicleFuelSection(vehicle: Vehicle, navController: NavController, vm: MainViewModel, dark: Boolean) {
    val avgMpg  by vm.avgMpg(vehicle.id).collectAsStateWithLifecycle(null)
    val entries by vm.fuelForVehicle(vehicle.id).collectAsStateWithLifecycle(emptyList())
    val amber   = if (dark) AmberDark else AmberLight
    val on      = if (dark) DarkTextPrimary else LightTextPrimary
    val sub     = if (dark) DarkTextSecondary else LightTextSecondary
    val bg      = if (dark) DarkCard else LightCard
    val bord    = if (dark) DarkCardBorder else LightCardBorder

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(bg).border(1.dp, bord, RoundedCornerShape(14.dp)).padding(14.dp)) {

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("${vehicle.make} ${vehicle.model}", fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, color = on)
                Text(avgMpg?.toDouble()?.let { "Avg: ${"%.1f".format(it)} MPG" } ?: "No fuel data",
                    color = sub, fontSize = 12.sp)
            }
            IconButton(onClick = { navController.navigate(Screen.AddEditFuel.createRoute(vehicle.id)) }) {
                Icon(Icons.Filled.Add, "Add Fuel", tint = amber)
            }
        }

        HorizontalDivider(color = bord, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

        if (entries.isEmpty()) {
            Text("No fuel logs yet", color = if (dark) DarkTextMuted else LightTextMuted,
                modifier = Modifier.padding(vertical = 6.dp))
        } else {
            entries.take(10).forEachIndexed { i, entry ->
                if (i > 0) Spacer(Modifier.height(6.dp))
                FuelEntryItem(entry, dark,
                    onClick  = { navController.navigate(Screen.AddEditFuel.createRoute(vehicle.id, entry.id)) },
                    onDelete = { vm.deleteFuelEntry(entry) })
            }
        }
    }
}

@Composable
fun FuelEntryItem(entry: FuelEntry, dark: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    val amber  = if (dark) AmberDark else AmberLight
    val green  = if (dark) GreenDark else GreenAccent
    val red    = if (dark) RedDark   else RedAccent
    val on     = if (dark) DarkTextPrimary else LightTextPrimary
    val sub    = if (dark) DarkTextSecondary else LightTextSecondary
    val rowBg  = if (dark) DarkSurface else Color(0xFFF8FAFC)
    val bord   = if (dark) DarkCardBorder else LightCardBorder

    if (showDelete) {
        AlertDialog(onDismissRequest = { showDelete = false },
            containerColor = if (dark) DarkCard else LightCard,
            title = { Text("Delete Entry", color = on) },
            text  = { Text("Delete this fuel log entry?", color = sub) },
            confirmButton = { TextButton(onClick = { onDelete(); showDelete = false }) { Text("Delete", color = red) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel", color = sub) } }
        )
    }

    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(rowBg)
        .border(1.dp, bord, RoundedCornerShape(10.dp)).clickable { onClick() }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.LocalGasStation, null, tint = amber, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(dateFmt.format(Date(entry.date)), fontWeight = FontWeight.SemiBold, color = on, fontSize = 13.sp)
            Text("${"%.1f".format(entry.litresFilled)}L  ·  ${entry.mileageAtFill} mi", fontSize = 12.sp, color = sub)
            if (entry.mpg > 0) {
                val mpgColor = when { entry.mpg >= 40 -> green; entry.mpg >= 30 -> amber; else -> red }
                Text("${"%.1f".format(entry.mpg)} MPG", color = mpgColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(ccyFmt.format(entry.totalCost), fontWeight = FontWeight.Bold, color = on, fontSize = 13.sp)
        IconButton(onClick = { showDelete = true }) {
            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp), tint = sub.copy(alpha = 0.5f))
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// SERVICES SCREEN
// ────────────────────────────────────────────────────────────────────────
@Composable
fun ServicesScreen(navController: NavController, vm: MainViewModel = hiltViewModel()) {
    val dark        = isDark()
    val predictions by vm.servicePredictions.collectAsStateWithLifecycle()
    val bgPage      = if (dark) DarkBg    else LightBg
    val amber       = if (dark) AmberDark else AmberLight
    val on          = if (dark) DarkTextPrimary else LightTextPrimary
    val sub         = if (dark) DarkTextSecondary else LightTextSecondary
    val bord        = if (dark) DarkCardBorder else LightCardBorder

    val overdueCount  = predictions.count { it.isOverdue || it.daysUntilDue <= 7 }
    val dueSoonCount  = predictions.count { !it.isOverdue && it.daysUntilDue in 8..30 }
    val upcomingCount = predictions.count { it.daysUntilDue > 30 }

    Scaffold(
        topBar         = { AutoTrackTopBar(title = "Upcoming Services") },
        bottomBar      = { AutoTrackBottomBar(navController) },
        containerColor = bgPage
    ) { padding ->
        if (predictions.isEmpty()) {
            Box(Modifier.fillMaxSize().carbonFibreBackground(dark).padding(padding), Alignment.Center) {
                Text("Add vehicles to see service predictions",
                    color = if (dark) DarkTextMuted else LightTextMuted)
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().carbonFibreBackground(dark).padding(padding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = amber)
                        Spacer(Modifier.width(8.dp))
                        Text("Smart Service Prediction", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = on)
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = bord, thickness = 0.5.dp)
                    Spacer(Modifier.height(10.dp))
                }

                // Summary strip
                item {
                    ServiceSummaryStrip(overdueCount, dueSoonCount, upcomingCount, dark)
                }

                items(predictions) { pred ->
                    ServicePredictionCard(pred, dark) {
                        vm.insertRecord(ServiceRecord(
                            vehicleId   = pred.vehicle.id,
                            serviceType = pred.serviceType,
                            date        = System.currentTimeMillis(),
                            mileage     = pred.vehicle.mileage,
                            cost        = 0.0
                        ))
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceSummaryStrip(overdueCount: Int, dueSoonCount: Int, upcomingCount: Int, dark: Boolean) {
    val red   = if (dark) RedDark   else RedAccent
    val amber = if (dark) AmberDark else AmberLight
    val green = if (dark) GreenDark else GreenAccent
    val bg    = if (dark) DarkCard  else LightCard
    val bord  = if (dark) DarkCardBorder else LightCardBorder
    val on    = if (dark) DarkTextPrimary else LightTextPrimary

    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg)
        .border(1.dp, bord, RoundedCornerShape(12.dp)).padding(vertical = 14.dp),
        Arrangement.SpaceEvenly) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(overdueCount.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = red)
            Text("OVERDUE", fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = on)
        }
        Box(Modifier.width(1.dp).height(40.dp).background(bord))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dueSoonCount.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = amber)
            Text("DUE SOON", fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = on)
        }
        Box(Modifier.width(1.dp).height(40.dp).background(bord))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(upcomingCount.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = green)
            Text("UPCOMING", fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = on)
        }
    }
}

@Composable
fun ServicePredictionCard(pred: ServicePrediction, dark: Boolean, onMarkComplete: () -> Unit) {
    val red   = if (dark) RedDark   else RedAccent
    val amber = if (dark) AmberDark else AmberLight
    val green = if (dark) GreenDark else GreenAccent
    val bg    = if (dark) DarkCard  else LightCard
    val bord  = if (dark) DarkCardBorder else LightCardBorder
    val on    = if (dark) DarkTextPrimary else LightTextPrimary
    val sub   = if (dark) DarkTextSecondary else LightTextSecondary

    val urgencyColor = when {
        pred.isOverdue          -> red
        pred.daysUntilDue <= 7  -> red
        pred.daysUntilDue <= 30 -> amber
        else                    -> green
    }

    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg)
        .border(1.dp, bord, RoundedCornerShape(14.dp))) {
        // Coloured left border
        Box(Modifier.width(4.dp).fillMaxHeight().align(Alignment.CenterStart)
            .background(urgencyColor, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))

        Column(Modifier.padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("${pred.vehicle.make} ${pred.vehicle.model}",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp, color = on)
                    Text(pred.serviceType, fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp, color = urgencyColor)
                }
                UrgencyBadge(pred.daysUntilDue)
            }
            Spacer(Modifier.height(6.dp))
            Text("Due: ${dateFmt.format(Date(pred.predictedDueDate))}  ·  ~${pred.predictedDueMileage} mi",
                fontSize = 12.sp, color = sub)
            Text("[${pred.confidence} confidence]", fontSize = 11.sp, color = sub.copy(alpha = 0.7f))
            Spacer(Modifier.height(10.dp))
            Button(onClick = onMarkComplete, modifier = Modifier.fillMaxWidth(),
                shape  = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = urgencyColor,
                    contentColor   = if (dark) DarkBg else Color.White)
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Mark Complete", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// ANALYTICS SCREEN
// ────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController, vm: MainViewModel = hiltViewModel()) {
    val dark     = isDark()
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val records  by vm.allRecords.collectAsStateWithLifecycle()
    val bgPage   = if (dark) DarkBg    else LightBg
    val amber    = if (dark) AmberDark else AmberLight
    val green    = if (dark) GreenDark else GreenAccent
    val red      = if (dark) RedDark   else RedAccent
    val on       = if (dark) DarkTextPrimary else LightTextPrimary
    val sub      = if (dark) DarkTextSecondary else LightTextSecondary
    val bg       = if (dark) DarkCard  else LightCard
    val bord     = if (dark) DarkCardBorder else LightCardBorder

    var selectedVehicleId by remember { mutableStateOf<Long?>(null) }
    var expanded          by remember { mutableStateOf(false) }

    val filteredRecords = if (selectedVehicleId != null)
        records.filter { it.vehicleId == selectedVehicleId } else records
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }
    val costByType      = filteredRecords
        .groupBy { it.serviceType }
        .mapValues { (_, v) -> v.sumOf { it.cost } }
        .entries.sortedByDescending { it.value }
    val totalSpend       = filteredRecords.sumOf { it.cost }
    val avgCost          = if (filteredRecords.isNotEmpty()) totalSpend / filteredRecords.size else 0.0
    val barColors        = listOf(amber, green, red, Color(0xFF8B5CF6), Color(0xFF0EA5E9))

    Scaffold(
        topBar         = { AutoTrackTopBar(title = "Analytics") },
        bottomBar      = { AutoTrackBottomBar(navController) },
        containerColor = bgPage
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().carbonFibreBackground(dark).padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Vehicle filter
            item {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedButton(
                        onClick  = { expanded = true },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Text(selectedVehicle?.let { "${it.make} ${it.model}" } ?: "All Vehicles",
                            color = amber, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowDropDown, null, tint = amber)
                    }
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                        modifier = Modifier.background(bg)) {
                        DropdownMenuItem(text = { Text("All Vehicles", color = on) },
                            onClick = { selectedVehicleId = null; expanded = false })
                        vehicles.forEach { v ->
                            DropdownMenuItem(text = { Text("${v.make} ${v.model}", color = on) },
                                onClick = { selectedVehicleId = v.id; expanded = false })
                        }
                    }
                }
            }

            // 4 KPI tiles
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiTile("Total Spend",  ccyFmt.format(totalSpend),          amber,              Modifier.weight(1f))
                        KpiTile("Records",      filteredRecords.size.toString(),     green,              Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiTile("Avg Cost",     ccyFmt.format(avgCost),             red,                Modifier.weight(1f))
                        KpiTile("Vehicles",     if (selectedVehicleId == null) vehicles.size.toString() else "1",
                            Color(0xFF8B5CF6),  Modifier.weight(1f))
                    }
                }
            }

            // Spend by service type bar chart
            item {
                AnalyticsCard("SPEND BY SERVICE TYPE", dark, sub) {
                    if (costByType.isEmpty()) {
                        Text("No service data yet", color = sub, modifier = Modifier.padding(8.dp))
                    } else {
                        val maxCost = costByType.maxOf { it.value }
                        costByType.forEachIndexed { i, (type, cost) ->
                            val barColor = barColors[i % barColors.size]
                            Column(Modifier.padding(vertical = 5.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text(type, fontSize = 13.sp, color = on)
                                    Text(ccyFmt.format(cost), fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp, color = barColor)
                                }
                                Spacer(Modifier.height(4.dp))
                                // Bar
                                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                    .background(bord)) {
                                    Box(Modifier.fillMaxWidth((cost / maxCost).toFloat()).fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Brush.horizontalGradient(listOf(barColor, barColor.copy(alpha = 0.5f)))))
                                }
                            }
                        }
                    }
                }
            }

            // MPG per vehicle bar chart
            if (vehicles.isNotEmpty()) {
                item {
                    AnalyticsCard("MPG PER VEHICLE", dark, sub) {
                        val displayVehicles = if (selectedVehicleId != null)
                            vehicles.filter { it.id == selectedVehicleId } else vehicles
                        MpgBarChart(displayVehicles, vm, dark, amber, green, red, on, bord, barColors)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(title: String, dark: Boolean, sub: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(if (dark) DarkCard else LightCard)
        .border(1.dp, if (dark) DarkCardBorder else LightCardBorder, RoundedCornerShape(14.dp))
        .padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold, color = sub)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun MpgBarChart(
    vehicles: List<Vehicle>, vm: MainViewModel, dark: Boolean,
    amber: Color, green: Color, red: Color, on: Color, bord: Color, barColors: List<Color>
) {
    vehicles.forEachIndexed { i, v ->
        val avgMpg by vm.avgMpg(v.id).collectAsStateWithLifecycle(null)
        val barColor = barColors[i % barColors.size]
        val mpgVal   = avgMpg?.toDouble()

        Column(Modifier.padding(vertical = 5.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("${v.make} ${v.model}", fontSize = 13.sp, color = on)
                Text(mpgVal?.let { "${"%.1f".format(it)} MPG" } ?: "—",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = barColor)
            }
            Spacer(Modifier.height(4.dp))
            val fraction = mpgVal?.let { (it / 60.0).coerceIn(0.0, 1.0).toFloat() } ?: 0f
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(bord)) {
                if (fraction > 0f) {
                    Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(barColor, barColor.copy(alpha = 0.5f)))))
                }
            }
        }
    }
}