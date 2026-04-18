package com.autotrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autotrack.data.local.entity.FuelEntry
import com.autotrack.data.local.entity.ServiceRecord
import com.autotrack.data.local.entity.Vehicle
import com.autotrack.data.local.repository.AutoTrackRepository
import com.autotrack.data.local.repository.NhtsaMake
import com.autotrack.data.local.repository.NhtsaModel
import com.autotrack.data.local.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs

// Service prediction data class
data class ServicePrediction(
    val vehicle: Vehicle,
    val serviceType: String,
    val predictedDueDate: Long,
    val predictedDueMileage: Int,
    val confidence: String,  // HIGH / MED / LOW
    val isOverdue: Boolean,
    val daysUntilDue: Int
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: AutoTrackRepository,
    val prefsRepo: PreferencesRepository
) : ViewModel() {

    //  Vehicles
    val vehicles: StateFlow<List<Vehicle>> = repo.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //  All records
    val allRecords: StateFlow<List<ServiceRecord>> = repo.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //  Preferences
    val preferences = prefsRepo.preferences
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            com.autotrack.data.local.repository.AppPreferences()
        )

    //  NHTSA makes/models
    private val _makes = MutableStateFlow<List<NhtsaMake>>(emptyList())
    val makes: StateFlow<List<NhtsaMake>> = _makes

    private val _models = MutableStateFlow<List<NhtsaModel>>(emptyList())
    val models: StateFlow<List<NhtsaModel>> = _models

    private val _isLoadingMakes = MutableStateFlow(false)
    val isLoadingMakes: StateFlow<Boolean> = _isLoadingMakes

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError

    fun loadMakes() = viewModelScope.launch {
        _isLoadingMakes.value = true
        _apiError.value = null
        try {
            val result = repo.fetchAllMakes()
            if (result.isEmpty()) {
                _apiError.value = "Failed to load makes. Check connection."
            }
            _makes.value = result.filter { it.MakeName != null }.sortedBy { it.MakeName }
        } catch (e: Exception) {
            _apiError.value = "Error: ${e.message}"
        } finally {
            _isLoadingMakes.value = false
        }
    }

    private var loadModelsJob: kotlinx.coroutines.Job? = null
    fun loadModels(make: String) {
        loadModelsJob?.cancel()
        if (make.isBlank()) {
            _models.value = emptyList()
            return
        }

        // Only trigger API if the make exists in our list (minimizes wasteful calls)
        val matches = _makes.value.any { it.MakeName?.equals(make, ignoreCase = true) == true }
        if (!matches) return

        loadModelsJob = viewModelScope.launch {
            _isLoadingModels.value = true
            _apiError.value = null
            try {
                val result = repo.fetchModelsForMake(make)
                _models.value = result.filter { it.ModelName != null }.sortedBy { it.ModelName }
            } catch (e: Exception) {
                _apiError.value = "Error: ${e.message}"
            } finally {
                _isLoadingModels.value = false
            }
        }
    }

    //  Vehicle CRUD
    fun insertVehicle(vehicle: Vehicle, onDone: (Long) -> Unit = {}) =
        viewModelScope.launch { onDone(repo.insertVehicle(vehicle)) }

    fun updateVehicle(vehicle: Vehicle) =
        viewModelScope.launch { repo.updateVehicle(vehicle) }

    fun deleteVehicle(vehicle: Vehicle) =
        viewModelScope.launch { repo.deleteVehicle(vehicle) }

    //  Record CRUD
    fun insertRecord(record: ServiceRecord) =
        viewModelScope.launch {
            repo.insertRecord(record)
            // Update vehicle mileage if record mileage is higher
            val vehicle = vehicles.value.find { it.id == record.vehicleId }
            if (vehicle != null && record.mileage > vehicle.mileage) {
                repo.updateMileage(record.vehicleId, record.mileage)
            }
        }

    fun updateRecord(record: ServiceRecord) =
        viewModelScope.launch { repo.updateRecord(record) }

    fun deleteRecord(record: ServiceRecord) =
        viewModelScope.launch { repo.deleteRecord(record) }

    //  Fuel CRUD
    fun insertFuelEntry(entry: FuelEntry) =
        viewModelScope.launch { repo.insertFuelEntry(entry) }

    fun updateFuelEntry(entry: FuelEntry) =
        viewModelScope.launch { repo.updateFuelEntry(entry) }

    fun deleteFuelEntry(entry: FuelEntry) =
        viewModelScope.launch { repo.deleteFuelEntry(entry) }

    // Per-vehicle flows
    fun recordsForVehicle(vehicleId: Long) = repo.getRecordsForVehicle(vehicleId)
    fun fuelForVehicle(vehicleId: Long)    = repo.getFuelEntries(vehicleId)
    fun totalSpend(vehicleId: Long)        = repo.getTotalSpend(vehicleId)
    fun avgMpg(vehicleId: Long)            = repo.getAverageMpg(vehicleId)
    fun recordCount(vehicleId: Long)       = repo.getRecordCount(vehicleId)
    fun fuelCount(vehicleId: Long)         = repo.getFuelCount(vehicleId)

    //  Smart Service Predictions
    val servicePredictions: StateFlow<List<ServicePrediction>> = combine(
        vehicles, allRecords
    ) { vehicleList, records ->
        generatePredictions(vehicleList, records)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val defaultIntervals = mapOf(
        "Oil Change"    to Pair(365, 10000),
        "Tyre Rotation" to Pair(180, 8000),
        "MOT"           to Pair(365, 0),
        "Brake Check"   to Pair(365, 20000),
        "Air Filter"    to Pair(730, 20000),
        "Coolant"       to Pair(730, 30000)
    )

    private fun generatePredictions(
        vehicleList: List<Vehicle>,
        records: List<ServiceRecord>
    ): List<ServicePrediction> {
        val now   = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val predictions = mutableListOf<ServicePrediction>()

        for (vehicle in vehicleList) {
            val vehicleRecords = records.filter { it.vehicleId == vehicle.id }

            for ((serviceType, interval) in defaultIntervals) {
                val lastRecord = vehicleRecords
                    .filter { it.serviceType == serviceType }
                    .maxByOrNull { it.date }

                val (intervalDays, intervalMiles) = interval

                val predictedDate: Long
                val predictedMileage: Int

                if (lastRecord != null) {
                    predictedDate    = lastRecord.date + (intervalDays * dayMs)
                    predictedMileage = lastRecord.mileage + intervalMiles
                } else {
                    // No history — estimate from vehicle age
                    val cal = Calendar.getInstance()
                    cal.set(vehicle.year, 0, 1)
                    predictedDate    = cal.timeInMillis + (intervalDays * dayMs)
                    predictedMileage = vehicle.mileage + intervalMiles
                }

                val daysUntil = ((predictedDate - now) / dayMs).toInt()
                val isOverdue = daysUntil < 0

                // Confidence logic:
                // HIGH = we have actual service history for this type
                // MED  = no history but prediction is within half the interval
                // LOW  = no history and prediction is far out
                val confidence = when {
                    lastRecord != null                 -> "HIGH"
                    abs(daysUntil) < intervalDays / 2 -> "MED"
                    else                               -> "LOW"
                }

                predictions.add(
                    ServicePrediction(
                        vehicle             = vehicle,
                        serviceType         = serviceType,
                        predictedDueDate    = predictedDate,
                        predictedDueMileage = predictedMileage,
                        confidence          = confidence,
                        isOverdue           = isOverdue,
                        daysUntilDue        = daysUntil
                    )
                )
            }
        }
        return predictions.sortedBy { it.daysUntilDue }
    }

    // Vehicle Health Score (0–100)
    fun healthScore(vehicleId: Long): Int {
        val preds = servicePredictions.value.filter { it.vehicle.id == vehicleId }
        var score = 100
        for (p in preds) {
            score -= when {
                p.isOverdue         -> 15
                p.daysUntilDue < 7  -> 10
                p.daysUntilDue < 30 -> 5
                else                -> 0
            }
        }
        return score.coerceIn(0, 100)
    }

    // Preferences updates
    fun setDarkTheme(enabled: Boolean) =
        viewModelScope.launch { prefsRepo.updateDarkTheme(enabled) }

    fun setDistanceUnit(unit: String) =
        viewModelScope.launch { prefsRepo.updateDistanceUnit(unit) }

    fun setCurrency(currency: String) =
        viewModelScope.launch { prefsRepo.updateCurrency(currency) }

    fun setRemindersEnabled(enabled: Boolean) =
        viewModelScope.launch { prefsRepo.updateRemindersEnabled(enabled) }

    fun setReminderInterval(interval: String) =
        viewModelScope.launch { prefsRepo.updateReminderInterval(interval) }

    fun setMileageAlerts(enabled: Boolean) =
        viewModelScope.launch { prefsRepo.updateMileageAlerts(enabled) }

    fun setOverdueThreshold(days: Int) =
        viewModelScope.launch { prefsRepo.updateOverdueThreshold(days) }
}