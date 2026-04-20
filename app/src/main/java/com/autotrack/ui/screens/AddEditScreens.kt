package com.autotrack.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.autotrack.data.local.entity.FuelEntry
import com.autotrack.data.local.entity.ServiceRecord
import com.autotrack.data.local.entity.Vehicle
import com.autotrack.ui.components.AutoTrackTopBar
import com.autotrack.ui.theme.*
import com.autotrack.util.createImageUri
import com.autotrack.util.fetchLocation
import com.autotrack.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val dateDisplayFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

val SERVICE_TYPES = listOf(
    "Oil Change", "Tyre Rotation", "MOT", "Brake Check",
    "Air Filter", "Coolant", "Battery", "Suspension", "Other"
)

@Composable
fun premiumTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor         = GoldPrimary,
    unfocusedBorderColor       = GunmetalLight,
    focusedLabelColor          = GoldPrimary,
    unfocusedLabelColor        = SilverDim,
    focusedTextColor           = ChromeWhite,
    unfocusedTextColor         = ChromeWhite,
    cursorColor                = GoldPrimary,
    focusedContainerColor      = GunmetalMid,
    unfocusedContainerColor    = GunmetalDeep,
    errorBorderColor           = CrimsonAlert,
    errorLabelColor            = CrimsonAlert,
    errorTextColor             = ChromeWhite,
    errorContainerColor        = GunmetalDeep,
    focusedTrailingIconColor   = GoldPrimary,
    unfocusedTrailingIconColor = SilverDim
)

@Composable
fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        color         = GoldPrimary,
        modifier      = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

//
// ADD / EDIT VEHICLE SCREEN
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddEditVehicleScreen(
    navController: NavController,
    vehicleId: Long? = null,
    vm: MainViewModel = hiltViewModel()
) {
    val focusManager   = LocalFocusManager.current
    val context        = LocalContext.current
    val makes          by vm.makes.collectAsStateWithLifecycle()
    val models         by vm.models.collectAsStateWithLifecycle()
    val isLoadingMakes by vm.isLoadingMakes.collectAsStateWithLifecycle()
    val vehicles       by vm.vehicles.collectAsStateWithLifecycle()
    val existing       = vehicles.find { it.id == vehicleId }

    var make      by remember { mutableStateOf(existing?.make     ?: "") }
    var model     by remember { mutableStateOf(existing?.model    ?: "") }
    var year      by remember { mutableStateOf(existing?.year?.toString() ?: "") }
    var mileage   by remember { mutableStateOf(existing?.mileage?.toString() ?: "") }
    var nickname  by remember { mutableStateOf(existing?.nickname ?: "") }
    var colour    by remember { mutableStateOf(existing?.colour   ?: "") }
    var fuelType  by remember { mutableStateOf(existing?.fuelType ?: "Petrol") }
    var photoUri  by remember { mutableStateOf(existing?.photoUri ?: "") }

    var makeExpanded     by remember { mutableStateOf(false) }
    var modelExpanded    by remember { mutableStateOf(false) }
    var fuelTypeExpanded by remember { mutableStateOf(false) }
    var makeQuery        by remember { mutableStateOf(make) }

    var makeError    by remember { mutableStateOf("") }
    var modelError   by remember { mutableStateOf("") }
    var yearError    by remember { mutableStateOf("") }
    var mileageError by remember { mutableStateOf("") }

    val isFormValid = make.isNotBlank() && model.isNotBlank() &&
            year.toIntOrNull() != null && mileage.toIntOrNull() != null

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    // Camera permission
    val cameraPermission = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // Camera + gallery launchers
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCameraUri?.let { photoUri = it.toString() }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { photoUri = it.toString() } }

    LaunchedEffect(Unit) { if (makes.isEmpty()) vm.loadMakes() }
    LaunchedEffect(make) { if (make.isNotBlank()) vm.loadModels(make) }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val fuelTypes   = listOf("Petrol", "Diesel", "Electric", "Hybrid", "LPG")

    fun validate(): Boolean {
        makeError    = if (make.isBlank()) "Make is required" else ""
        modelError   = if (model.isBlank()) "Model is required" else ""
        yearError    = year.toIntOrNull()?.let {
            if (it < 1900 || it > currentYear + 1) "Year must be 1900–$currentYear" else ""
        } ?: "Enter a valid year"
        mileageError = mileage.toIntOrNull()?.let {
            if (it < 0) "Mileage must be ≥ 0" else ""
        } ?: "Enter valid mileage"
        return listOf(makeError, modelError, yearError, mileageError).all { it.isEmpty() }
    }

    val fieldColors = premiumTextFieldColors()

    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title    = if (vehicleId != null) "EDIT VEHICLE" else "ADD VEHICLE",
                showBack = true,
                onBack   = { navController.popBackStack() }
            )
        },
        containerColor = Obsidian,
        snackbarHost   = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().background(Obsidian).padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionLabel("VEHICLE DETAILS") }

            item {
                ExposedDropdownMenuBox(expanded = makeExpanded, onExpandedChange = { makeExpanded = it }) {
                    OutlinedTextField(
                        value          = makeQuery,
                        onValueChange  = { makeQuery = it; make = it; makeExpanded = true },
                        label          = { Text("Make *") },
                        isError        = makeError.isNotEmpty(),
                        supportingText = { if (makeError.isNotEmpty()) Text(makeError, color = CrimsonAlert) },
                        trailingIcon   = {
                            if (isLoadingMakes) CircularProgressIndicator(Modifier.size(20.dp), color = GoldPrimary, strokeWidth = 2.dp)
                            else ExposedDropdownMenuDefaults.TrailingIcon(makeExpanded)
                        },
                        modifier        = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine      = true,
                        colors          = fieldColors,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    val filtered = makes.filter {
                        (it.MakeName ?: "").contains(makeQuery, ignoreCase = true)
                    }.take(20)
                    if (filtered.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded         = makeExpanded,
                            onDismissRequest = { makeExpanded = false },
                            modifier         = Modifier.background(GunmetalMid)
                        ) {
                            filtered.forEach { m ->
                                DropdownMenuItem(
                                    text    = { Text(m.MakeName ?: "", color = ChromeWhite) },
                                    onClick = {
                                        make = m.MakeName ?: ""
                                        makeQuery = m.MakeName ?: ""
                                        model = ""
                                        makeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { if (models.isNotEmpty()) modelExpanded = it }) {
                    OutlinedTextField(
                        value          = model,
                        onValueChange  = { model = it },
                        label          = { Text("Model *") },
                        isError        = modelError.isNotEmpty(),
                        supportingText = { if (modelError.isNotEmpty()) Text(modelError, color = CrimsonAlert) },
                        trailingIcon   = { ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) },
                        modifier       = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine     = true,
                        colors         = fieldColors,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    ExposedDropdownMenu(
                        expanded         = modelExpanded,
                        onDismissRequest = { modelExpanded = false },
                        modifier         = Modifier.background(GunmetalMid)
                    ) {
                        models.filter {
                            (it.ModelName ?: "").contains(model, ignoreCase = true)
                        }.take(20).forEach { m ->
                            DropdownMenuItem(
                                text    = { Text(m.ModelName ?: "", color = ChromeWhite) },
                                onClick = { model = m.ModelName ?: ""; modelExpanded = false }
                            )
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = year, onValueChange = { year = it }, label = { Text("Year *") },
                        isError = yearError.isNotEmpty(),
                        supportingText = { if (yearError.isNotEmpty()) Text(yearError, color = CrimsonAlert) },
                        modifier = Modifier.weight(1f), singleLine = true, colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    OutlinedTextField(
                        value = mileage, onValueChange = { mileage = it }, label = { Text("Mileage *") },
                        isError = mileageError.isNotEmpty(),
                        supportingText = { if (mileageError.isNotEmpty()) Text(mileageError, color = CrimsonAlert) },
                        modifier = Modifier.weight(1f), singleLine = true, colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = colour, onValueChange = { colour = it }, label = { Text("Colour") },
                        modifier = Modifier.weight(1f), singleLine = true, colors = fieldColors,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    OutlinedTextField(
                        value = nickname, onValueChange = { nickname = it }, label = { Text("Nickname") },
                        modifier = Modifier.weight(1f), singleLine = true, colors = fieldColors,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }
            }

            item {
                ExposedDropdownMenuBox(expanded = fuelTypeExpanded, onExpandedChange = { fuelTypeExpanded = it }) {
                    OutlinedTextField(
                        value = fuelType, onValueChange = {}, label = { Text("Fuel Type") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(fuelTypeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), colors = fieldColors
                    )
                    ExposedDropdownMenu(
                        expanded         = fuelTypeExpanded,
                        onDismissRequest = { fuelTypeExpanded = false },
                        modifier         = Modifier.background(GunmetalMid)
                    ) {
                        fuelTypes.forEach { ft ->
                            DropdownMenuItem(
                                text    = { Text(ft, color = ChromeWhite) },
                                onClick = { fuelType = ft; fuelTypeExpanded = false }
                            )
                        }
                    }
                }
            }

            // Vehicle photo
            item { SectionLabel("VEHICLE PHOTO") }

            item {
                if (photoUri.isNotBlank()) {
                    AsyncImage(
                        model              = photoUri,
                        contentDescription = "Vehicle photo",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(GunmetalMid, RoundedCornerShape(12.dp))
                            .border(1.dp, GoldDim.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (cameraPermission.status.isGranted) {
                                val uri = createImageUri(context)
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermission.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, GoldDim)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick  = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, GoldDim)
                    ) {
                        Icon(Icons.Filled.Photo, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery", fontSize = 12.sp)
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (validate()) {
                            val vehicle = Vehicle(
                                id       = vehicleId ?: 0L,
                                make     = make,
                                model    = model,
                                year     = year.toInt(),
                                mileage  = mileage.toIntOrNull() ?: 0,
                                nickname = nickname,
                                colour   = colour,
                                fuelType = fuelType,
                                photoUri = photoUri
                            )
                            if (vehicleId != null) vm.updateVehicle(vehicle)
                            else vm.insertVehicle(vehicle)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (vehicleId != null) "Vehicle updated successfully"
                                    else "Vehicle saved successfully"
                                )
                            }
                            navController.popBackStack()
                        }
                    },
                    enabled  = isFormValid,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = GoldPrimary,
                        contentColor           = Obsidian,
                        disabledContainerColor = GunmetalLight,
                        disabledContentColor   = SilverDim
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (vehicleId != null) "UPDATE VEHICLE" else "SAVE VEHICLE",
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 13.sp
                    )
                }
                if (!isFormValid) {
                    Text(
                        "Fill in Make, Model, Year and Mileage to continue",
                        fontSize = 11.sp, color = SilverDim,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}


// ADD / EDIT SERVICE RECORD SCREEN
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddEditRecordScreen(
    navController: NavController,
    vehicleId: Long,
    recordId: Long? = null,
    vm: MainViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val context      = LocalContext.current
    val vehicles     by vm.vehicles.collectAsStateWithLifecycle()
    val vehicle      = vehicles.find { it.id == vehicleId }
    val records      by vm.recordsForVehicle(vehicleId).collectAsStateWithLifecycle(emptyList())
    val existing     = records.find { it.id == recordId }

    var serviceType    by remember { mutableStateOf(existing?.serviceType ?: SERVICE_TYPES[0]) }
    var mileage        by remember { mutableStateOf(existing?.mileage?.toString() ?: "") }
    var cost           by remember { mutableStateOf(existing?.cost?.toString() ?: "") }
    var garage         by remember { mutableStateOf(existing?.garage ?: "") }
    var notes          by remember { mutableStateOf(existing?.notes ?: "") }
    var receiptUri     by remember { mutableStateOf(existing?.receiptPhotoUri ?: "") }
    var isLocating     by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = existing?.date ?: System.currentTimeMillis()
    )
    val selectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
    val displayDate = remember(selectedDateMillis) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
    }

    var mileageError by remember { mutableStateOf("") }
    var costError    by remember { mutableStateOf("") }

    val isFormValid = mileage.toIntOrNull() != null && cost.toDoubleOrNull() != null

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    // Camera permission
    val cameraPermission = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // GPS permission
    val locationPermission = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Camera + gallery launchers
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCameraUri?.let { receiptUri = it.toString() }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { receiptUri = it.toString() } }

    fun validate(): Boolean {
        mileageError = mileage.toIntOrNull()?.let { if (it < 0) "Must be ≥ 0" else "" } ?: "Enter valid mileage"
        costError    = cost.toDoubleOrNull()?.let { if (it < 0) "Must be ≥ 0" else "" } ?: "Enter valid cost"
        return listOf(mileageError, costError).all { it.isEmpty() }
    }

    val fieldColors = premiumTextFieldColors()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = { TextButton(onClick = { showDatePicker = false }) { Text("OK", color = GoldPrimary) } },
            dismissButton    = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = SilverMid) } },
            colors           = DatePickerDefaults.colors(containerColor = GunmetalMid)
        ) {
            DatePicker(
                state  = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor            = GunmetalMid,
                    titleContentColor         = GoldPrimary,
                    headlineContentColor      = ChromeWhite,
                    weekdayContentColor       = SilverDim,
                    subheadContentColor       = SilverMid,
                    dayContentColor           = ChromeWhite,
                    selectedDayContainerColor = GoldPrimary,
                    selectedDayContentColor   = Obsidian,
                    todayContentColor         = GoldPrimary,
                    todayDateBorderColor      = GoldPrimary
                )
            )
        }
    }

    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title    = if (recordId != null) "EDIT RECORD" else "LOG SERVICE",
                showBack = true,
                onBack   = { navController.popBackStack() }
            )
        },
        containerColor = Obsidian,
        snackbarHost   = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().background(Obsidian).padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            vehicle?.let {
                item {
                    Text(
                        "${it.make} ${it.model}".uppercase(),
                        fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        letterSpacing = 0.8.sp, color = GoldPrimary
                    )
                }
            }

            item { SectionLabel("SERVICE TYPE") }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement   = Arrangement.spacedBy(6.dp)
                ) {
                    SERVICE_TYPES.forEach { type ->
                        val selected = serviceType == type
                        Surface(
                            onClick = { serviceType = type },
                            color   = if (selected) GoldPrimary.copy(alpha = 0.15f) else GunmetalMid,
                            shape   = RoundedCornerShape(8.dp),
                            border  = androidx.compose.foundation.BorderStroke(
                                1.dp, if (selected) GoldPrimary else GunmetalLight
                            )
                        ) {
                            Text(
                                text       = type,
                                fontSize   = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (selected) GoldPrimary else SilverMid,
                                modifier   = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            item { SectionLabel("DETAILS") }

            item {
                OutlinedTextField(
                    value         = displayDate,
                    onValueChange = {},
                    label         = { Text("Date *") },
                    readOnly      = true,
                    trailingIcon  = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Pick date", tint = GoldPrimary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = fieldColors
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = mileage, onValueChange = { mileage = it }, label = { Text("Mileage *") },
                        isError = mileageError.isNotEmpty(),
                        supportingText = { if (mileageError.isNotEmpty()) Text(mileageError, color = CrimsonAlert) },
                        modifier = Modifier.weight(1f), singleLine = true, colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    OutlinedTextField(
                        value = cost, onValueChange = { cost = it }, label = { Text("Cost (£) *") },
                        isError = costError.isNotEmpty(),
                        supportingText = { if (costError.isNotEmpty()) Text(costError, color = CrimsonAlert) },
                        modifier = Modifier.weight(1f), singleLine = true, colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }
            }

            // Garage + GPS
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value           = garage,
                        onValueChange   = { garage = it },
                        label           = { Text("Garage / Location") },
                        modifier        = Modifier.weight(1f),
                        singleLine      = true,
                        colors          = fieldColors,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction      = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                    OutlinedButton(
                        onClick = {
                            if (locationPermission.status.isGranted) {
                                isLocating = true
                                fetchLocation(context) { address ->
                                    garage     = address
                                    isLocating = false
                                }
                            } else {
                                locationPermission.launchPermissionRequest()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GoldPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldDim)
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(14.dp),
                                color       = GoldPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = "Use GPS",
                                modifier           = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("GPS", fontSize = 12.sp)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it }, label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().height(100.dp), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            // Receipt photo
            item { SectionLabel("RECEIPT PHOTO") }

            item {
                if (receiptUri.isNotBlank()) {
                    AsyncImage(
                        model              = receiptUri,
                        contentDescription = "Receipt photo",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(GunmetalMid, RoundedCornerShape(12.dp))
                            .border(1.dp, GoldDim.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (cameraPermission.status.isGranted) {
                                val uri = createImageUri(context)
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermission.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, GoldDim)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick  = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, GoldDim)
                    ) {
                        Icon(Icons.Filled.Photo, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery", fontSize = 12.sp)
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (validate()) {
                            val record = ServiceRecord(
                                id              = recordId ?: 0L,
                                vehicleId       = vehicleId,
                                serviceType     = serviceType,
                                date            = selectedDateMillis,
                                mileage         = mileage.toIntOrNull() ?: 0,
                                cost            = cost.toDoubleOrNull() ?: 0.0,
                                garage          = garage,
                                notes           = notes,
                                receiptPhotoUri = receiptUri
                            )
                            if (recordId != null) vm.updateRecord(record)
                            else vm.insertRecord(record)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (recordId != null) "Record updated"
                                    else "$serviceType logged successfully"
                                )
                            }
                            navController.popBackStack()
                        }
                    },
                    enabled  = isFormValid,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = GoldPrimary,
                        contentColor           = Obsidian,
                        disabledContainerColor = GunmetalLight,
                        disabledContentColor   = SilverDim
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (recordId != null) "UPDATE RECORD" else "SAVE RECORD",
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 13.sp
                    )
                }
                if (!isFormValid) {
                    Text(
                        "Enter mileage and cost to continue",
                        fontSize = 11.sp, color = SilverDim,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}


// ADD / EDIT FUEL ENTRY SCREEN
@Composable
fun AddEditFuelScreen(
    navController: NavController,
    vehicleId: Long,
    entryId: Long? = null,
    vm: MainViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val vehicles     by vm.vehicles.collectAsStateWithLifecycle()
    val vehicle      = vehicles.find { it.id == vehicleId }
    val entries      by vm.fuelForVehicle(vehicleId).collectAsStateWithLifecycle(emptyList())
    val existing     = entries.find { it.id == entryId }

    var litres       by remember { mutableStateOf(existing?.litresFilled?.toString() ?: "") }
    var costPerLitre by remember { mutableStateOf(existing?.costPerLitre?.toString() ?: "") }
    var mileage      by remember { mutableStateOf(existing?.mileageAtFill?.toString() ?: "") }
    var fuelType     by remember { mutableStateOf(existing?.fuelType ?: "Petrol") }
    var isFullTank   by remember { mutableStateOf(existing?.isFullTank ?: true) }
    var notes        by remember { mutableStateOf(existing?.notes ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = existing?.date ?: System.currentTimeMillis()
    )
    val selectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
    val displayDate = remember(selectedDateMillis) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
    }

    var fuelTypeExpanded by remember { mutableStateOf(false) }
    var litresError  by remember { mutableStateOf("") }
    var costError    by remember { mutableStateOf("") }
    var mileageError by remember { mutableStateOf("") }

    val totalCost = (litres.toDoubleOrNull() ?: 0.0) * (costPerLitre.toDoubleOrNull() ?: 0.0)
    val fuelTypes = listOf("Petrol", "Diesel", "Electric", "Hybrid", "LPG")

    val isFormValid = litres.toDoubleOrNull() != null &&
            costPerLitre.toDoubleOrNull() != null &&
            mileage.toIntOrNull() != null

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    fun validate(): Boolean {
        litresError  = litres.toDoubleOrNull()?.let { if (it <= 0) "Must be > 0" else "" } ?: "Required"
        costError    = costPerLitre.toDoubleOrNull()?.let { if (it <= 0) "Must be > 0" else "" } ?: "Required"
        mileageError = mileage.toIntOrNull()?.let { if (it < 0) "Must be ≥ 0" else "" } ?: "Required"
        return listOf(litresError, costError, mileageError).all { it.isEmpty() }
    }

    val fieldColors = premiumTextFieldColors()

    if (showDatePicker) {
        @OptIn(ExperimentalMaterial3Api::class)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = { TextButton(onClick = { showDatePicker = false }) { Text("OK", color = GoldPrimary) } },
            dismissButton    = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = SilverMid) } },
            colors           = DatePickerDefaults.colors(containerColor = GunmetalMid)
        ) {
            @OptIn(ExperimentalMaterial3Api::class)
            DatePicker(
                state  = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor            = GunmetalMid,
                    titleContentColor         = GoldPrimary,
                    headlineContentColor      = ChromeWhite,
                    weekdayContentColor       = SilverDim,
                    selectedDayContainerColor = GoldPrimary,
                    selectedDayContentColor   = Obsidian,
                    todayContentColor         = GoldPrimary,
                    todayDateBorderColor      = GoldPrimary
                )
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            AutoTrackTopBar(
                title    = if (entryId != null) "EDIT FUEL ENTRY" else "LOG FUEL",
                showBack = true,
                onBack   = { navController.popBackStack() }
            )
        },
        containerColor = Obsidian,
        snackbarHost   = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().background(Obsidian).padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            vehicle?.let {
                item {
                    Text(
                        "${it.make} ${it.model}".uppercase(),
                        fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        letterSpacing = 0.8.sp, color = GoldPrimary
                    )
                }
            }

            item { SectionLabel("FILL-UP DETAILS") }

            item {
                OutlinedTextField(
                    value         = displayDate,
                    onValueChange = {},
                    label         = { Text("Date *") },
                    readOnly      = true,
                    trailingIcon  = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Pick date", tint = GoldPrimary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = fieldColors
                )
            }

            item {
                OutlinedTextField(
                    value = mileage, onValueChange = { mileage = it },
                    label = { Text("Mileage at Fill *") },
                    isError = mileageError.isNotEmpty(),
                    supportingText = { if (mileageError.isNotEmpty()) Text(mileageError, color = CrimsonAlert) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = litres, onValueChange = { litres = it }, label = { Text("Litres *") },
                        isError = litresError.isNotEmpty(),
                        supportingText = { if (litresError.isNotEmpty()) Text(litresError, color = CrimsonAlert) },
                        modifier = Modifier.weight(1f), singleLine = true, colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    OutlinedTextField(
                        value = costPerLitre, onValueChange = { costPerLitre = it },
                        label = { Text("Cost/Litre *") },
                        isError = costError.isNotEmpty(),
                        supportingText = { if (costError.isNotEmpty()) Text(costError, color = CrimsonAlert) },
                        modifier = Modifier.weight(1f), singleLine = true, colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }
            }

            if (totalCost > 0) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GunmetalMid, RoundedCornerShape(12.dp))
                            .border(1.dp, GoldDim.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("TOTAL COST", fontSize = 10.sp, letterSpacing = 1.5.sp, color = SilverDim)
                            Text("£${"%.2f".format(totalCost)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GoldPrimary)
                        }
                    }
                }
            }

            item {
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(expanded = fuelTypeExpanded, onExpandedChange = { fuelTypeExpanded = it }) {
                    OutlinedTextField(
                        value = fuelType, onValueChange = {}, readOnly = true,
                        label = { Text("Fuel Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(fuelTypeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), colors = fieldColors
                    )
                    ExposedDropdownMenu(
                        expanded         = fuelTypeExpanded,
                        onDismissRequest = { fuelTypeExpanded = false },
                        modifier         = Modifier.background(GunmetalMid)
                    ) {
                        fuelTypes.forEach { ft ->
                            DropdownMenuItem(
                                text    = { Text(ft, color = ChromeWhite) },
                                onClick = { fuelType = ft; fuelTypeExpanded = false }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .background(GunmetalMid, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Checkbox(
                        checked         = isFullTank,
                        onCheckedChange = { isFullTank = it },
                        colors          = CheckboxDefaults.colors(
                            checkedColor   = GoldPrimary,
                            uncheckedColor = SilverDim
                        )
                    )
                    Text("Full tank fill-up", color = ChromeWhite, fontSize = 14.sp)
                }
            }

            item {
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it }, label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (validate()) {
                            val litresVal = litres.toDoubleOrNull() ?: 0.0
                            val cplVal    = costPerLitre.toDoubleOrNull() ?: 0.0
                            val lastEntry = entries.firstOrNull()
                            val mpg = if (lastEntry != null && isFullTank && lastEntry.isFullTank) {
                                val milesDriven = (mileage.toIntOrNull() ?: 0) - lastEntry.mileageAtFill
                                if (milesDriven > 0 && litresVal > 0)
                                    milesDriven / (litresVal * 0.2199692)
                                else 0.0
                            } else 0.0
                            val entry = FuelEntry(
                                id            = entryId ?: 0L,
                                vehicleId     = vehicleId,
                                date          = selectedDateMillis,
                                litresFilled  = litresVal,
                                costPerLitre  = cplVal,
                                totalCost     = litresVal * cplVal,
                                mileageAtFill = mileage.toIntOrNull() ?: 0,
                                fuelType      = fuelType,
                                isFullTank    = isFullTank,
                                notes         = notes,
                                mpg           = mpg
                            )
                            if (entryId != null) vm.updateFuelEntry(entry)
                            else vm.insertFuelEntry(entry)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (entryId != null) "Fuel entry updated"
                                    else "Fuel log saved successfully"
                                )
                            }
                            navController.popBackStack()
                        }
                    },
                    enabled  = isFormValid,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = GoldPrimary,
                        contentColor           = Obsidian,
                        disabledContainerColor = GunmetalLight,
                        disabledContentColor   = SilverDim
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (entryId != null) "UPDATE ENTRY" else "SAVE FUEL LOG",
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 13.sp
                    )
                }
                if (!isFormValid) {
                    Text(
                        "Enter litres, cost per litre and mileage to continue",
                        fontSize = 11.sp, color = SilverDim,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}