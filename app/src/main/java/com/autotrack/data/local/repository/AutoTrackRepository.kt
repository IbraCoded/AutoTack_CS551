package com.autotrack.data.local.repository

import com.autotrack.data.local.dao.FuelEntryDao
import com.autotrack.data.local.dao.ServiceRecordDao
import com.autotrack.data.local.dao.VehicleDao
import com.autotrack.data.local.entity.FuelEntry
import com.autotrack.data.local.entity.ServiceRecord
import com.autotrack.data.local.entity.Vehicle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoTrackRepository @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val recordDao: ServiceRecordDao,
    private val fuelDao: FuelEntryDao,
    private val nhtsaApi: NhtsaApiService
) {
    // Vehicles
    fun getAllVehicles() = vehicleDao.getAllVehicles()
    suspend fun insertVehicle(v: Vehicle) = vehicleDao.insertVehicle(v)
    suspend fun updateVehicle(v: Vehicle) = vehicleDao.updateVehicle(v)
    suspend fun deleteVehicle(v: Vehicle) = vehicleDao.deleteVehicle(v)
    suspend fun updateMileage(id: Long, m: Int) = vehicleDao.updateMileage(id, m)

    // Records
    fun getAllRecords() = recordDao.getAllRecords()
    fun getRecordsForVehicle(id: Long) = recordDao.getRecordsForVehicle(id)
    fun getTotalSpend(id: Long) = recordDao.getTotalSpend(id)
    fun getRecordCount(id: Long) = recordDao.getRecordCount(id)
    suspend fun insertRecord(r: ServiceRecord) = recordDao.insertRecord(r)
    suspend fun updateRecord(r: ServiceRecord) = recordDao.updateRecord(r)
    suspend fun deleteRecord(r: ServiceRecord) = recordDao.deleteRecord(r)

    // Fuel
    fun getFuelEntries(id: Long) = fuelDao.getFuelEntries(id)
    fun getAverageMpg(id: Long) = fuelDao.getAverageMpg(id)
    fun getFuelCount(id: Long) = fuelDao.getFuelCount(id)
    suspend fun insertFuelEntry(e: FuelEntry) = fuelDao.insertFuelEntry(e)
    suspend fun updateFuelEntry(e: FuelEntry) = fuelDao.updateFuelEntry(e)
    suspend fun deleteFuelEntry(e: FuelEntry) = fuelDao.deleteFuelEntry(e)

    // NHTSA API
    suspend fun fetchAllMakes(): List<NhtsaMake> =
        runCatching { nhtsaApi.getAllMakes().Results }
            .getOrDefault(emptyList())

    suspend fun fetchModelsForMake(make: String): List<NhtsaModel> =
        runCatching { nhtsaApi.getModelsForMake(make).Results }
            .getOrDefault(emptyList())
}