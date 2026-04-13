package com.autotrack.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.autotrack.data.local.entity.Vehicle
import com.autotrack.navigation.Screen
import com.autotrack.ui.components.AutoTrackBottomBar
import com.autotrack.ui.components.AutoTrackTopBar
import com.autotrack.ui.components.HealthScoreRing
import com.autotrack.ui.components.UrgencyBadge
import com.autotrack.ui.theme.RedAlert
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
                title        = "AutoTrack",
                showSettings = true,
                onSettings   = { navController.navigate(Screen.Preferences.route) }
            )
        },
        bottomBar = { AutoTrackBottomBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddEditVehicle.createRoute()) }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Vehicle")
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No vehicles yet",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap + to add your first vehicle",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.padding(padding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "MY VEHICLES",
                        style    = MaterialTheme.typography.labelLarge,
                        color    = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(vehicles, key = { it.id }) { vehicle ->
                    val score       = vm.healthScore(vehicle.id)
                    val worstDays   = predictions
                        .filter { it.vehicle.id == vehicle.id }
                        .minByOrNull { it.daysUntilDue }
                        ?.daysUntilDue
                    VehicleCard(
                        vehicle     = vehicle,
                        score       = score,
                        worstDays   = worstDays,
                        onClick     = {
                            navController.navigate(
                                Screen.VehicleDetail.createRoute(vehicle.id)
                            )
                        },
                        onLongClick = {
                            navController.navigate(
                                Screen.AddEditVehicle.createRoute(vehicle.id)
                            )
                        },
                        onDelete    = { vm.deleteVehicle(vehicle) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
            title   = { Text("Delete Vehicle") },
            text    = {
                Text("Delete ${vehicle.make} ${vehicle.model}? " +
                        "All records will be removed.")
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = RedAlert)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick     = onClick,
                    onLongClick = onLongClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HealthScoreRing(score = score, size = 72f)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${vehicle.make} ${vehicle.model}",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
                Text(
                    "${vehicle.year} · ${vehicle.mileage} mi",
                    color    = MaterialTheme.colorScheme.outline,
                    fontSize = 13.sp
                )
                if (!vehicle.nickname.isNullOrBlank()) {
                    Text(
                        vehicle.nickname,
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.height(6.dp))
                if (worstDays != null) UrgencyBadge(worstDays)
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint               = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}