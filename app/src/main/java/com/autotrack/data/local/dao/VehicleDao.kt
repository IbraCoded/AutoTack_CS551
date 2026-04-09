package com.autotrack.data.local.dao

import androidx.room.*
import com.autotrack.data.local.entity.Vehicle
import kotlinx.coroutines.flow.Flow
@Dao
interface VehicleDao {

    @Query("SELECT * FROM vehicles ORDER BY make ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Long): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("UPDATE vehicles SET mileage = :mileage WHERE id = :vehicleId")
    suspend fun updateMileage(vehicleId: Long, mileage: Int)
}