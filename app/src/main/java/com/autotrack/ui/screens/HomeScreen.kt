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
import com.autotrack.data.local.entity.Vehicle
import com.autotrack.navigation.Screen
import com.autotrack.ui.components.*
import com.autotrack.ui.theme.*
import com.autotrack.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    vm: MainViewModel = hiltViewModel()
) {
    val vehicles    by vm.vehicles.collectAsStateWithLifecycle()
    val predictions by vm.servicePredictions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title        = "AUTOTRACK",
                showSettings = true,
                onSettings   = { navController.navigate(Screen.Preferences.route) }
            )
        },
        bottomBar            = { AutoTrackBottomBar(navController) },
        containerColor       = Obsidian,
        floatingActionButton = {
            FloatingActionButton(
                onClick          = { navController.navigate(Screen.AddEditVehicle.createRoute()) },
                containerColor   = GoldPrimary,
                contentColor     = Obsidian,
                elevation        = FloatingActionButtonDefaults.elevation(8.dp, 12.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = "Add Vehicle") }
        }
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Obsidian),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NO VEHICLES YET",
                        style         = MaterialTheme.typography.labelLarge,
                        color         = SilverDim,
                        letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to add your first vehicle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverDim.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .background(Obsidian)
                    .padding(padding),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        "MY VEHICLES",
                        style         = MaterialTheme.typography.labelLarge,
                        color         = GoldPrimary,
                        letterSpacing = 2.sp,
                        modifier      = Modifier.padding(bottom = 6.dp, start = 2.dp)
                    )
                }
                items(vehicles, key = { it.id }) { vehicle ->
                    val score        = vm.healthScore(vehicle.id)
                    val vehiclePreds = predictions.filter { it.vehicle.id == vehicle.id }
                    val worstPred    = vehiclePreds.minByOrNull { it.daysUntilDue }
                    VehicleCard(
                        vehicle     = vehicle,
                        score       = score,
                        worstDays   = worstPred?.daysUntilDue,
                        onClick     = { navController.navigate(Screen.VehicleDetail.createRoute(vehicle.id)) },
                        onLongClick = { navController.navigate(Screen.AddEditVehicle.createRoute(vehicle.id)) },
                        onDelete    = { vm.deleteVehicle(vehicle) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleCard(
    vehicle: Vehicle,
    score: Int,
    worstDays: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = GunmetalMid,
            titleContentColor = ChromeWhite,
            textContentColor  = SilverMid,
            title            = { Text("Delete Vehicle") },
            text             = {
                Text("Delete ${vehicle.make} ${vehicle.model}? All records will be permanently removed.")
            },
            confirmButton    = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("DELETE", color = CrimsonAlert, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL", color = SilverMid, letterSpacing = 1.sp)
                }
            }
        )
    }

    // Determine card accent by health
    val accentColor = when {
        score >= 70 -> EmeraldSuccess
        score >= 40 -> AmberWarn
        else        -> CrimsonAlert
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GunmetalDeep)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.5f),
                        GunmetalLight,
                        GunmetalLight
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
    ) {
        // Subtle left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor.copy(alpha = 0.1f), accentColor, accentColor.copy(alpha = 0.1f))
                    ),
                    RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                )
        )

        Row(
            modifier          = Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Health ring
            HealthScoreRing(score = score, size = 72f)

            Spacer(Modifier.width(16.dp))

            // Vehicle info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${vehicle.make} ${vehicle.model}".uppercase(),
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 14.sp,
                    letterSpacing = 0.8.sp,
                    color         = ChromeWhite
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${vehicle.year}  ·  ${vehicle.mileage} mi",
                    color    = SilverDim,
                    fontSize = 12.sp,
                    letterSpacing = 0.3.sp
                )
                if (!vehicle.nickname.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        vehicle.nickname!!,
                        fontSize      = 11.sp,
                        color         = GoldPrimary.copy(alpha = 0.8f),
                        fontWeight    = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (worstDays != null) UrgencyBadge(worstDays)
            }

            // Delete button
            IconButton(
                onClick  = { showDeleteDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint               = SilverDim.copy(alpha = 0.5f),
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}
