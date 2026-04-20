package com.autotrack.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autotrack.data.local.entity.FuelEntry
import com.autotrack.data.local.entity.ServiceRecord
import com.autotrack.data.local.entity.Vehicle
import com.autotrack.data.local.repository.AutoTrackRepository
import com.autotrack.data.local.repository.NhtsaMake
import com.autotrack.data.local.repository.NhtsaModel
import com.autotrack.data.local.repository.PreferencesRepository
import com.autotrack.notifications.scheduleServiceReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs

data class ServicePrediction(
    val vehicle: Vehicle,
    val serviceType: String,
    val predictedDueDate: Long,
    val predictedDueMileage: Int,
    val confidence: String,
    val isOverdue: Boolean,
    val daysUntilDue: Int
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: AutoTrackRepository,
    val prefsRepo: PreferencesRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = repo.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecords: StateFlow<List<ServiceRecord>> = repo.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences = prefsRepo.preferences
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.autotrack.data.local.repository.AppPreferences()
        )

    private val _makes = MutableStateFlow<List<NhtsaMake>>(emptyList())
    val makes: StateFlow<List<NhtsaMake>> = _makes

    private val _models = MutableStateFlow<List<NhtsaModel>>(emptyList())
    val models: StateFlow<List<NhtsaModel>> = _models

    private val _isLoadingMakes = MutableStateFlow(false)
    val isLoadingMakes: StateFlow<Boolean> = _isLoadingMakes

    fun loadMakes() = viewModelScope.launch {
        _isLoadingMakes.value = true
        _makes.value = repo.fetchAllMakes().sortedBy { it.MakeName ?: "" }
        _isLoadingMakes.value = false
    }

    fun loadModels(make: String) = viewModelScope.launch {
        _models.value = repo.fetchModelsForMake(make).sortedBy { it.ModelName ?: "" }
    }

    fun insertVehicle(vehicle: Vehicle, onDone: (Long) -> Unit = {}) =
        viewModelScope.launch { onDone(repo.insertVehicle(vehicle)) }

    fun updateVehicle(vehicle: Vehicle) =
        viewModelScope.launch { repo.updateVehicle(vehicle) }

    fun deleteVehicle(vehicle: Vehicle) =
        viewModelScope.launch { repo.deleteVehicle(vehicle) }

    fun insertRecord(record: ServiceRecord) =
        viewModelScope.launch {
            repo.insertRecord(record)
            val vehicle = vehicles.value.find { it.id == record.vehicleId }
            if (vehicle != null && record.mileage > vehicle.mileage) {
                repo.updateMileage(record.vehicleId, record.mileage)
            }
        }

    fun updateRecord(record: ServiceRecord) =
        viewModelScope.launch { repo.updateRecord(record) }

    fun deleteRecord(record: ServiceRecord) =
        viewModelScope.launch { repo.deleteRecord(record) }

    fun insertFuelEntry(entry: FuelEntry) =
        viewModelScope.launch { repo.insertFuelEntry(entry) }

    fun updateFuelEntry(entry: FuelEntry) =
        viewModelScope.launch { repo.updateFuelEntry(entry) }

    fun deleteFuelEntry(entry: FuelEntry) =
        viewModelScope.launch { repo.deleteFuelEntry(entry) }

    fun recordsForVehicle(vehicleId: Long) = repo.getRecordsForVehicle(vehicleId)
    fun fuelForVehicle(vehicleId: Long) = repo.getFuelEntries(vehicleId)
    fun totalSpend(vehicleId: Long) = repo.getTotalSpend(vehicleId)
    fun avgMpg(vehicleId: Long) = repo.getAverageMpg(vehicleId)
    fun recordCount(vehicleId: Long) = repo.getRecordCount(vehicleId)
    fun fuelCount(vehicleId: Long) = repo.getFuelCount(vehicleId)

    val servicePredictions: StateFlow<List<ServicePrediction>> = combine(
        vehicles, allRecords
    ) { vehicleList, records ->
        generatePredictions(vehicleList, records)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val defaultIntervals = mapOf(
        "Oil Change" to Pair(365, 10000),
        "Tyre Rotation" to Pair(180, 8000),
        "MOT" to Pair(365, 0),
        "Brake Check" to Pair(365, 20000),
        "Air Filter" to Pair(730, 20000),
        "Coolant" to Pair(730, 30000)
    )

    private fun generatePredictions(
        vehicleList: List<Vehicle>,
        records: List<ServiceRecord>
    ): List<ServicePrediction> {
        val now = System.currentTimeMillis()
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
                    predictedDate = lastRecord.date + (intervalDays * dayMs)
                    predictedMileage = lastRecord.mileage + intervalMiles
                } else {
                    // FALLBACK: When no history exists, predict from NOW or next major milestone.
                    // This prevents "unrealistic" ancient dates (e.g., from 2011) for older cars.
                    predictedDate = now + (intervalDays * dayMs)
                    
                    predictedMileage = if (intervalMiles > 0) {
                        // Round up to the next major interval milestone based on current mileage
                        ((vehicle.mileage / intervalMiles) + 1) * intervalMiles
                    } else {
                        vehicle.mileage
                    }
                }

                val daysUntil = ((predictedDate - now) / dayMs).toInt()
                val isOverdue = daysUntil < 0
                val confidence = when {
                    lastRecord != null -> "HIGH"
                    abs(daysUntil) < intervalDays / 2 -> "MED"
                    else -> "LOW"
                }

                predictions.add(
                    ServicePrediction(
                        vehicle = vehicle,
                        serviceType = serviceType,
                        predictedDueDate = predictedDate,
                        predictedDueMileage = predictedMileage,
                        confidence = confidence,
                        isOverdue = isOverdue,
                        daysUntilDue = daysUntil
                    )
                )
            }
        }
        return predictions.sortedBy { it.daysUntilDue }
    }

    fun healthScore(vehicleId: Long): Int {
        vehicles.value.find { it.id == vehicleId } ?: return 50
        val preds = servicePredictions.value.filter { it.vehicle.id == vehicleId }
        var score = 100
        for (p in preds) {
            score -= when {
                p.isOverdue -> 15
                p.daysUntilDue < 7 -> 10
                p.daysUntilDue < 30 -> 5
                else -> 0
            }
        }
        return score.coerceIn(0, 100)
    }

    fun setDarkTheme(enabled: Boolean) =
        viewModelScope.launch { prefsRepo.updateDarkTheme(enabled) }

    fun setDistanceUnit(unit: String) = viewModelScope.launch { prefsRepo.updateDistanceUnit(unit) }
    fun setCurrency(currency: String) = viewModelScope.launch { prefsRepo.updateCurrency(currency) }
    fun setRemindersEnabled(enabled: Boolean) =
        viewModelScope.launch { prefsRepo.updateRemindersEnabled(enabled) }

    fun setReminderInterval(interval: String) =
        viewModelScope.launch {
            prefsRepo.updateReminderInterval(interval)
            val days = when (interval) {
                "Daily" -> 1L
                "Weekly" -> 7L
                "Fortnightly" -> 14L
                "Monthly" -> 30L
                else -> 7L
            }
            scheduleServiceReminder(appContext, days)
        }

    fun setMileageAlerts(enabled: Boolean) =
        viewModelScope.launch { prefsRepo.updateMileageAlerts(enabled) }

    fun setOverdueThreshold(days: Int) =
        viewModelScope.launch { prefsRepo.updateOverdueThreshold(days) }

    fun setShakeEnabled(enabled: Boolean) =
        viewModelScope.launch { prefsRepo.updateShakeEnabled(enabled) }
}