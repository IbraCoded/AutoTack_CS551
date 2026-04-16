package com.autotrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.autotrack.data.local.entity.FuelEntry
import com.autotrack.data.local.entity.ServiceRecord
import com.autotrack.data.local.entity.Vehicle
import com.autotrack.ui.components.AutoTrackTopBar
import com.autotrack.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateDisplayFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

val SERVICE_TYPES = listOf(
    "Oil Change", "Tyre Rotation", "MOT", "Brake Check",
    "Air Filter", "Coolant", "Battery", "Suspension", "Other"
)


// ADD / EDIT VEHICLE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleScreen(
    navController: NavController,
    vehicleId: Long? = null,
    vm: MainViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val makes by vm.makes.collectAsStateWithLifecycle()
    val models by vm.models.collectAsStateWithLifecycle()
    val isLoadingMakes by vm.isLoadingMakes.collectAsStateWithLifecycle()
    val isLoadingModels by vm.isLoadingModels.collectAsStateWithLifecycle()
    val apiError by vm.apiError.collectAsStateWithLifecycle()
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val existing = vehicles.find { it.id == vehicleId }

    var make by remember { mutableStateOf(existing?.make ?: "") }
    var model by remember { mutableStateOf(existing?.model ?: "") }
    var year by remember { mutableStateOf(existing?.year?.toString() ?: "") }
    var mileage by remember { mutableStateOf(existing?.mileage?.toString() ?: "") }
    var nickname by remember { mutableStateOf(existing?.nickname ?: "") }
    var colour by remember { mutableStateOf(existing?.colour ?: "") }
    var fuelType by remember { mutableStateOf(existing?.fuelType ?: "Petrol") }

    var makeExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var fuelTypeExpanded by remember { mutableStateOf(false) }
    var makeQuery by remember { mutableStateOf(existing?.make ?: "") }

    var makeError by remember { mutableStateOf("") }
    var modelError by remember { mutableStateOf("") }
    var yearError by remember { mutableStateOf("") }
    var mileageError by remember { mutableStateOf("") }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val fuelTypes = listOf("Petrol", "Diesel", "Electric", "Hybrid", "LPG")

    LaunchedEffect(Unit) {
        if (makes.isEmpty()) vm.loadMakes()
    }
    LaunchedEffect(make) {
        if (make.isNotBlank()) vm.loadModels(make)
    }

    fun validate(): Boolean {
        makeError = if (make.isBlank()) "Make is required" else ""
        modelError = if (model.isBlank()) "Model is required" else ""
        yearError = year.toIntOrNull()?.let {
            if (it < 1900 || it > currentYear + 1)
                "Year must be 1900–$currentYear" else ""
        } ?: "Enter a valid year"
        mileageError = mileage.toIntOrNull()?.let {
            if (it < 0) "Must be ≥ 0" else ""
        } ?: "Enter valid mileage"
        return listOf(makeError, modelError, yearError, mileageError)
            .all { it.isEmpty() }
    }

    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title = if (vehicleId != null) "Edit Vehicle" else "Add Vehicle",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            //Make autocomplete
            item {
                ExposedDropdownMenuBox(
                    expanded = makeExpanded,
                    onExpandedChange = { makeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = makeQuery,
                        onValueChange = {
                            makeQuery = it
                            make = it
                            makeExpanded = true
                        },
                        label = { Text("Make *") },
                        isError = makeError.isNotEmpty() || apiError != null,
                        supportingText = {
                            if (makeError.isNotEmpty()) Text(makeError)
                            else if (apiError != null) Text(apiError!!, color = MaterialTheme.colorScheme.error)
                        },
                        trailingIcon = {
                            if (isLoadingMakes)
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else
                                ExposedDropdownMenuDefaults.TrailingIcon(makeExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                    val filtered = makes.filter {
                        val q = makeQuery.trim()
                        if (q.isEmpty()) true
                        else it.MakeName?.contains(q, ignoreCase = true) == true
                    }.take(30)

                    if (filtered.isNotEmpty() && makeExpanded) {
                        ExposedDropdownMenu(
                            expanded = makeExpanded,
                            onDismissRequest = { makeExpanded = false }
                        ) {
                            filtered.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.MakeName ?: "") },
                                    onClick = {
                                        make = m.MakeName ?: ""
                                        makeQuery = m.MakeName ?: ""
                                        model = ""
                                        makeExpanded = false
                                        focusManager.moveFocus(FocusDirection.Down)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            //Model dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = {
                        if (models.isNotEmpty() || isLoadingModels) modelExpanded = it
                    }
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it; modelExpanded = true },
                        label = { Text("Model *") },
                        isError = modelError.isNotEmpty(),
                        supportingText = {
                            if (modelError.isNotEmpty()) Text(modelError)
                        },
                        trailingIcon = {
                            if (isLoadingModels)
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else
                                ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                    val filteredModels = models.filter {
                        it.ModelName?.contains(model, ignoreCase = true) == true
                    }.take(30)

                    if (filteredModels.isNotEmpty() && modelExpanded) {
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            filteredModels.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.ModelName ?: "") },
                                    onClick = {
                                        model = m.ModelName ?: ""
                                        modelExpanded = false
                                        focusManager.moveFocus(FocusDirection.Down)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            //Year + Mileage
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Year *") },
                        isError = yearError.isNotEmpty(),
                        supportingText = {
                            if (yearError.isNotEmpty()) Text(yearError)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                    OutlinedTextField(
                        value = mileage,
                        onValueChange = { mileage = it },
                        label = { Text("Mileage *") },
                        isError = mileageError.isNotEmpty(),
                        supportingText = {
                            if (mileageError.isNotEmpty()) Text(mileageError)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }
            }

            // Colour + Nickname
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = colour,
                        onValueChange = { colour = it },
                        label = { Text("Colour") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Nickname") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }
            }

            // Fuel type
            item {
                ExposedDropdownMenuBox(
                    expanded = fuelTypeExpanded,
                    onExpandedChange = { fuelTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = fuelType,
                        onValueChange = {},
                        label = { Text("Fuel Type") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(fuelTypeExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = fuelTypeExpanded,
                        onDismissRequest = { fuelTypeExpanded = false }
                    ) {
                        fuelTypes.forEach { ft ->
                            DropdownMenuItem(
                                text = { Text(ft) },
                                onClick = {
                                    fuelType = ft
                                    fuelTypeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            //Save button
            item {
                Button(
                    onClick = {
                        if (validate()) {
                            val vehicle = Vehicle(
                                id = vehicleId ?: 0L,
                                make = make,
                                model = model,
                                year = year.toInt(),
                                mileage = mileage.toIntOrNull() ?: 0,
                                nickname = nickname,
                                colour = colour,
                                fuelType = fuelType
                            )
                            if (vehicleId != null) vm.updateVehicle(vehicle)
                            else vm.insertVehicle(vehicle)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (vehicleId != null) "Update Vehicle" else "Save Vehicle")
                }
            }
        }
    }
}


// ADD / EDIT SERVICE RECORD

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddEditRecordScreen(
    navController: NavController,
    vehicleId: Long,
    recordId: Long? = null,
    vm: MainViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val vehicle = vehicles.find { it.id == vehicleId }
    val records by vm.recordsForVehicle(vehicleId)
        .collectAsStateWithLifecycle(emptyList())
    val existing = records.find { it.id == recordId }

    var serviceType by remember {
        mutableStateOf(existing?.serviceType ?: SERVICE_TYPES[0])
    }
    var selectedDateMs by remember {
        mutableStateOf(existing?.date ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var mileage by remember {
        mutableStateOf(existing?.mileage?.toString() ?: "")
    }
    var cost by remember {
        mutableStateOf(existing?.cost?.toString() ?: "")
    }
    var garage by remember { mutableStateOf(existing?.garage ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    var mileageError by remember { mutableStateOf("") }
    var costError by remember { mutableStateOf("") }
    var showMileageWarning by remember { mutableStateOf(false) }

    // GPS permission
    val locationPermission = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    // DatePicker state — no future dates
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMs,
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= System.currentTimeMillis()
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMs = it
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Low mileage warning dialog
    if (showMileageWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showMileageWarning = false },
            title = { Text("Mileage Warning") },
            text = {
                Text(
                    "The mileage you entered is lower than a previous record. " +
                            "Are you sure?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showMileageWarning = false
                    saveRecord(
                        vm, vehicleId, recordId, serviceType,
                        selectedDateMs, mileage, cost, garage, notes,
                        navController
                    )
                }) { Text("Save Anyway") }
            },
            dismissButton = {
                TextButton(onClick = { showMileageWarning = false }) {
                    Text("Go Back")
                }
            }
        )
    }

    fun validate(): Boolean {
        mileageError = mileage.toIntOrNull()?.let {
            if (it < 0) "Must be ≥ 0" else ""
        } ?: "Enter valid mileage"
        costError = cost.toDoubleOrNull()?.let {
            if (it < 0) "Must be ≥ 0" else ""
        } ?: "Enter valid cost"
        return listOf(mileageError, costError).all { it.isEmpty() }
    }

    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title = if (recordId != null) "Edit Record" else "Log Service",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            vehicle?.let {
                item {
                    Text(
                        "${it.make} ${it.model}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Service type chips
            item {
                Text(
                    "Service Type *",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SERVICE_TYPES.forEach { type ->
                        FilterChip(
                            selected = serviceType == type,
                            onClick = { serviceType = type },
                            label = { Text(type) }
                        )
                    }
                }
            }

            // Date picker field
            item {
                OutlinedTextField(
                    value = dateDisplayFmt.format(Date(selectedDateMs)),
                    onValueChange = {},
                    label = { Text("Date *") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = "Pick date"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Mileage + Cost
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = mileage,
                        onValueChange = { mileage = it },
                        label = { Text("Mileage") },
                        isError = mileageError.isNotEmpty(),
                        supportingText = {
                            if (mileageError.isNotEmpty()) Text(mileageError)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = { Text("Cost (£)") },
                        isError = costError.isNotEmpty(),
                        supportingText = {
                            if (costError.isNotEmpty()) Text(costError)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }
            }

            // Garage + GPS button
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = garage,
                        onValueChange = { garage = it },
                        label = { Text("Garage / Location") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                    OutlinedButton(
                        onClick = {
                            if (locationPermission.status.isGranted) {
                                fetchLocation(context) { address ->
                                    garage = address
                                }
                            } else {
                                locationPermission.launchPermissionRequest()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Use GPS",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("GPS")
                    }
                }
            }

            // Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
            }

            // Save
            item {
                Button(
                    onClick = {
                        if (validate()) {
                            // Check if mileage is lower than previous records
                            val prevMileage = records
                                .filter { it.id != recordId }
                                .maxByOrNull { it.mileage }
                                ?.mileage ?: 0
                            val enteredMileage = mileage.toIntOrNull() ?: 0
                            if (enteredMileage > 0 && enteredMileage < prevMileage) {
                                showMileageWarning = true
                            } else {
                                saveRecord(
                                    vm, vehicleId, recordId, serviceType,
                                    selectedDateMs, mileage, cost, garage,
                                    notes, navController
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (recordId != null) "Update Record" else "Save Record")
                }
            }
        }
    }
}

private fun saveRecord(
    vm: MainViewModel,
    vehicleId: Long,
    recordId: Long?,
    serviceType: String,
    date: Long,
    mileage: String,
    cost: String,
    garage: String,
    notes: String,
    navController: NavController
) {
    val record = ServiceRecord(
        id = recordId ?: 0L,
        vehicleId = vehicleId,
        serviceType = serviceType,
        date = date,
        mileage = mileage.toIntOrNull() ?: 0,
        cost = cost.toDoubleOrNull() ?: 0.0,
        garage = garage,
        notes = notes
    )
    if (recordId != null) vm.updateRecord(record)
    else vm.insertRecord(record)
    navController.popBackStack()
}

// GPS reverse geocode helper
fun fetchLocation(
    context: android.content.Context,
    onResult: (String) -> Unit
) {
    val fusedClient = com.google.android.gms.location.LocationServices
        .getFusedLocationProviderClient(context)
    try {
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = android.location.Geocoder(
                    context, Locale.getDefault()
                )

                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(
                    location.latitude, location.longitude, 1
                )
                val address = addresses?.firstOrNull()
                val result = listOfNotNull(
                    address?.featureName,
                    address?.locality,
                    address?.adminArea
                ).joinToString(", ")
                onResult(result.ifBlank { "Unknown location" })
            }
        }
    } catch (e: SecurityException) {
        onResult("")
    }
}


// ADD / EDIT FUEL ENTRY

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFuelScreen(
    navController: NavController,
    vehicleId: Long,
    entryId: Long? = null,
    vm: MainViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val vehicle = vehicles.find { it.id == vehicleId }
    val entries by vm.fuelForVehicle(vehicleId)
        .collectAsStateWithLifecycle(emptyList())
    val existing = entries.find { it.id == entryId }

    var selectedDateMs by remember {
        mutableStateOf(existing?.date ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var litres by remember {
        mutableStateOf(existing?.litresFilled?.toString() ?: "")
    }
    var costPerLitre by remember {
        mutableStateOf(existing?.costPerLitre?.toString() ?: "")
    }
    var mileage by remember {
        mutableStateOf(existing?.mileageAtFill?.toString() ?: "")
    }
    var fuelType by remember {
        mutableStateOf(existing?.fuelType ?: "Petrol")
    }
    var isFullTank by remember {
        mutableStateOf(existing?.isFullTank ?: true)
    }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var fuelTypeExpanded by remember { mutableStateOf(false) }

    var litresError by remember { mutableStateOf("") }
    var costError by remember { mutableStateOf("") }
    var mileageError by remember { mutableStateOf("") }

    val totalCost = (litres.toDoubleOrNull() ?: 0.0) *
            (costPerLitre.toDoubleOrNull() ?: 0.0)
    val fuelTypes = listOf("Petrol", "Diesel", "Electric", "Hybrid", "LPG")

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMs,
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= System.currentTimeMillis()
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMs = it
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    fun validate(): Boolean {
        litresError = litres.toDoubleOrNull()?.let {
            if (it <= 0) "Must be > 0" else ""
        } ?: "Required"
        costError = costPerLitre.toDoubleOrNull()?.let {
            if (it <= 0) "Must be > 0" else ""
        } ?: "Required"
        mileageError = mileage.toIntOrNull()?.let {
            if (it < 0) "Must be ≥ 0" else ""
        } ?: "Required"
        return listOf(litresError, costError, mileageError).all { it.isEmpty() }
    }

    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title = if (entryId != null) "Edit Fuel Entry" else "Log Fuel",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            vehicle?.let {
                item {
                    Text(
                        "${it.make} ${it.model}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Date
            item {
                OutlinedTextField(
                    value = dateDisplayFmt.format(Date(selectedDateMs)),
                    onValueChange = {},
                    label = { Text("Date *") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = "Pick date"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Mileage
            item {
                OutlinedTextField(
                    value = mileage,
                    onValueChange = { mileage = it },
                    label = { Text("Mileage at fill *") },
                    isError = mileageError.isNotEmpty(),
                    supportingText = {
                        if (mileageError.isNotEmpty()) Text(mileageError)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
            }

            // Litres + Cost per litre
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = litres,
                        onValueChange = { litres = it },
                        label = { Text("Litres *") },
                        isError = litresError.isNotEmpty(),
                        supportingText = {
                            if (litresError.isNotEmpty()) Text(litresError)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                    OutlinedTextField(
                        value = costPerLitre,
                        onValueChange = { costPerLitre = it },
                        label = { Text("£/Litre *") },
                        isError = costError.isNotEmpty(),
                        supportingText = {
                            if (costError.isNotEmpty()) Text(costError)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }
            }

            // Auto-calculated total
            if (totalCost > 0) {
                item {
                    Card {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Cost (auto)")
                            Text(
                                "£${"%.2f".format(totalCost)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Fuel type dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = fuelTypeExpanded,
                    onExpandedChange = { fuelTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = fuelType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fuel Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(fuelTypeExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = fuelTypeExpanded,
                        onDismissRequest = { fuelTypeExpanded = false }
                    ) {
                        fuelTypes.forEach { ft ->
                            DropdownMenuItem(
                                text = { Text(ft) },
                                onClick = {
                                    fuelType = ft
                                    fuelTypeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Full tank checkbox
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isFullTank,
                        onCheckedChange = { isFullTank = it }
                    )
                    Text("Full tank fill-up")
                }
            }

            // Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
            }

            // Save
            item {
                Button(
                    onClick = {
                        if (validate()) {
                            val litresVal = litres.toDoubleOrNull() ?: 0.0
                            val cplVal = costPerLitre.toDoubleOrNull() ?: 0.0
                            val lastEntry = entries.firstOrNull()
                            val mpg = if (lastEntry != null &&
                                isFullTank && lastEntry.isFullTank
                            ) {
                                val milesDriven = (mileage.toIntOrNull() ?: 0) -
                                        lastEntry.mileageAtFill
                                if (milesDriven > 0 && litresVal > 0)
                                    milesDriven / (litresVal * 0.2199692)
                                else 0.0
                            } else 0.0

                            val entry = FuelEntry(
                                id = entryId ?: 0L,
                                vehicleId = vehicleId,
                                date = selectedDateMs,
                                litresFilled = litresVal,
                                costPerLitre = cplVal,
                                totalCost = litresVal * cplVal,
                                mileageAtFill = mileage.toIntOrNull() ?: 0,
                                fuelType = fuelType,
                                isFullTank = isFullTank,
                                notes = notes,
                                mpg = mpg
                            )
                            if (entryId != null) vm.updateFuelEntry(entry)
                            else vm.insertFuelEntry(entry)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (entryId != null) "Update Entry" else "Save Fuel Log")
                }
            }
        }
    }
}