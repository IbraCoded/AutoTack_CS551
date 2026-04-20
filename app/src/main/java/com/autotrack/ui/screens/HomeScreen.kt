package com.autotrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.autotrack.data.local.entity.Vehicle
import com.autotrack.navigation.Screen
import com.autotrack.ui.components.*
import com.autotrack.ui.theme.*
import com.autotrack.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

private val ccyFmt = NumberFormat.getCurrencyInstance(Locale.UK)

@Composable
fun HomeScreen(navController: NavController, vm: MainViewModel = hiltViewModel()) {
    val dark        = isDark()
    val vehicles    by vm.vehicles.collectAsStateWithLifecycle()
    val predictions by vm.servicePredictions.collectAsStateWithLifecycle()
    val allRecords  by vm.allRecords.collectAsStateWithLifecycle()
    val amber       = if (dark) AmberDark else AmberLight
    val bgPage      = if (dark) DarkBg    else LightBg

    Scaffold(
        topBar = {
            AutoTrackTopBar(title = "AUTOTRACK", showSettings = true,
                onSettings = { navController.navigate(Screen.Preferences.route) })
        },
        bottomBar      = { AutoTrackBottomBar(navController) },
        containerColor = bgPage,
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { navController.navigate(Screen.AddEditVehicle.createRoute()) },
                containerColor = amber,
                contentColor   = if (dark) DarkBg else Color.White,
                elevation      = FloatingActionButtonDefaults.elevation(6.dp)
            ) { Icon(Icons.Filled.Add, "Add Vehicle") }
        }
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(
                Modifier.fillMaxSize().carbonFibreBackground(dark).padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.DirectionsCar, null,
                        tint = amber.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                    Text("NO VEHICLES YET", style = MaterialTheme.typography.labelLarge,
                        color = if (dark) DarkTextMuted else LightTextMuted, letterSpacing = 2.sp)
                    Text("Tap + to add your first vehicle", style = MaterialTheme.typography.bodySmall,
                        color = (if (dark) DarkTextMuted else LightTextMuted).copy(alpha = 0.6f))
                }
            }
        } else {
            val primary   = vehicles.first()
            val rest      = vehicles.drop(1)
            val primRecs  = allRecords.filter { it.vehicleId == primary.id }
            val totalSpend = primRecs.sumOf { it.cost }

            LazyColumn(
                modifier            = Modifier.fillMaxSize().carbonFibreBackground(dark).padding(padding),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero card
                item {
                    val score by vm.getHealthScoreFlow(primary.id).collectAsStateWithLifecycle(100)
                    HeroVehicleCard(
                        vehicle     = primary,
                        score       = score,
                        worstDays   = predictions.filter { it.vehicle.id == primary.id }
                            .minByOrNull { it.daysUntilDue }?.daysUntilDue,
                        totalSpend  = totalSpend,
                        recordCount = primRecs.size,
                        dark        = dark,
                        onClick     = { navController.navigate(Screen.VehicleDetail.createRoute(primary.id)) },
                        onDelete    = { vm.deleteVehicle(primary) },
                        vm          = vm
                    )
                }

                if (rest.isNotEmpty()) {
                    item {
                        SectionLabel("OTHER VEHICLES")
                    }
                    items(rest, key = { it.id }) { vehicle ->
                        val score by vm.getHealthScoreFlow(vehicle.id).collectAsStateWithLifecycle(100)
                        val worstDay = predictions.filter { it.vehicle.id == vehicle.id }
                            .minByOrNull { it.daysUntilDue }?.daysUntilDue
                        CompactVehicleCard(
                            vehicle   = vehicle, score = score, worstDays = worstDay, dark = dark,
                            onClick   = { navController.navigate(Screen.VehicleDetail.createRoute(vehicle.id)) },
                            onDelete  = { vm.deleteVehicle(vehicle) }
                        )
                    }
                }
            }
        }
    }
}

// ── Hero Card ──────────────────────────────────────────────────────────
@Composable
fun HeroVehicleCard(
    vehicle: Vehicle, score: Int, worstDays: Int?, totalSpend: Double, recordCount: Int,
    dark: Boolean, onClick: () -> Unit, onDelete: () -> Unit, vm: MainViewModel
) {
    val avgMpg by vm.avgMpg(vehicle.id).collectAsStateWithLifecycle(null)
    var showDelete by remember { mutableStateOf(false) }
    if (showDelete) DeleteVehicleDialog(vehicle, { onDelete(); showDelete = false }, { showDelete = false }, dark)

    val amber = if (dark) AmberDark else AmberLight
    val bg    = if (dark) DarkCard  else LightCard
    val on    = if (dark) DarkTextPrimary else LightTextPrimary
    val sub   = if (dark) DarkTextSecondary else LightTextSecondary
    val green = if (dark) GreenDark else GreenAccent
    val red   = if (dark) RedDark   else RedAccent
    val acc   = when { score >= 70 -> green; score >= 40 -> amber; else -> red }

    Card(Modifier.fillMaxWidth().clickable { onClick() },
        shape  = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Photo / placeholder area
            Box(
                Modifier.fillMaxWidth().height(170.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(if (dark) DarkSurface else Color(0xFFF1F5F9))
            ) {
                if (!vehicle.photoUri.isNullOrBlank()) {
                    AsyncImage(model = vehicle.photoUri, contentDescription = "Vehicle photo",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(acc.copy(alpha = 0.15f), bg))
                        ), contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.DirectionsCar, null,
                            tint = acc.copy(alpha = 0.35f), modifier = Modifier.size(64.dp))
                    }
                }
                // Bottom fade
                Box(Modifier.fillMaxWidth().height(50.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, bg.copy(alpha = 0.9f)))))
                // Health ring bottom-right
                Box(Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                    HealthScoreRing(score = score, size = 62f)
                }
                // Urgency badge top-left
                if (worstDays != null) {
                    Box(Modifier.align(Alignment.TopStart).padding(10.dp)) { UrgencyBadge(worstDays) }
                }
                // Delete button top-right
                IconButton(onClick = { showDelete = true },
                    modifier = Modifier.align(Alignment.TopEnd).size(36.dp)) {
                    Icon(Icons.Filled.Delete, "Delete",
                        tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }

            // Info section
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("${vehicle.year} ${vehicle.make} ${vehicle.model}".uppercase(),
                    fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = 0.4.sp, color = on)
                if (vehicle.nickname.isNotBlank()) {
                    Text(vehicle.nickname, fontSize = 12.sp, color = amber, fontWeight = FontWeight.Medium)
                }
                Text("${vehicle.mileage} mi  ·  ${vehicle.fuelType}",
                    color = sub, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))

                Spacer(Modifier.height(12.dp))

                // Stat pills
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("SPEND",   ccyFmt.format(totalSpend),                      Modifier.weight(1f))
                    StatChip("AVG MPG", avgMpg?.toDouble()?.let { "%.1f".format(it) } ?: "—", Modifier.weight(1f))
                    StatChip("RECORDS", recordCount.toString(),                          Modifier.weight(1f))
                }
            }

            // Coloured accent strip at bottom
            Box(Modifier.fillMaxWidth().height(3.dp).background(
                Brush.horizontalGradient(listOf(acc.copy(alpha = 0.9f), acc.copy(alpha = 0.2f)))
            ))
        }
    }
}

// ── Compact card ───────────────────────────────────────────────────────
@Composable
fun CompactVehicleCard(
    vehicle: Vehicle, score: Int, worstDays: Int?, dark: Boolean,
    onClick: () -> Unit, onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    if (showDelete) DeleteVehicleDialog(vehicle, { onDelete(); showDelete = false }, { showDelete = false }, dark)

    val amber = if (dark) AmberDark else AmberLight
    val bg    = if (dark) DarkCard   else LightCard
    val bord  = if (dark) DarkCardBorder else LightCardBorder
    val on    = if (dark) DarkTextPrimary else LightTextPrimary
    val sub   = if (dark) DarkTextSecondary else LightTextSecondary
    val green = if (dark) GreenDark else GreenAccent
    val red   = if (dark) RedDark   else RedAccent
    val acc   = when { score >= 70 -> green; score >= 40 -> amber; else -> red }

    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg)
        .border(1.dp, bord, RoundedCornerShape(14.dp)).clickable { onClick() }
    ) {
        // Left accent bar
        Box(Modifier.width(4.dp).fillMaxHeight().align(Alignment.CenterStart)
            .background(Brush.verticalGradient(
                listOf(acc.copy(alpha = 0.1f), acc, acc.copy(alpha = 0.1f))),
                RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))

        Row(Modifier.padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            HealthScoreRing(score = score, size = 58f)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${vehicle.make} ${vehicle.model}".uppercase(),
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.4.sp, color = on)
                Text("${vehicle.year}  ·  ${vehicle.mileage} mi", color = sub, fontSize = 12.sp)
                if (vehicle.nickname.isNotBlank()) {
                    Text(vehicle.nickname, fontSize = 11.sp, color = amber.copy(alpha = 0.8f))
                }
                if (worstDays != null) { Spacer(Modifier.height(4.dp)); UrgencyBadge(worstDays) }
            }
            IconButton(onClick = { showDelete = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, "Delete", tint = sub.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Delete dialog ──────────────────────────────────────────────────────
@Composable
fun DeleteVehicleDialog(vehicle: Vehicle, onConfirm: () -> Unit, onDismiss: () -> Unit, dark: Boolean) {
    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = if (dark) DarkCard else LightCard,
        titleContentColor = if (dark) DarkTextPrimary else LightTextPrimary,
        textContentColor  = if (dark) DarkTextSecondary else LightTextSecondary,
        title  = { Text("Delete Vehicle") },
        text   = { Text("Delete ${vehicle.make} ${vehicle.model}? All records will be permanently removed.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("DELETE", color = RedAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = if (dark) DarkTextMuted else LightTextMuted)
            }
        }
    )
}
