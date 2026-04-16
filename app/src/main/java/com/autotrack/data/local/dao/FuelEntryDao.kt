package com.autotrack.data.local.dao

import androidx.room.*
import com.autotrack.data.local.entity.FuelEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelEntryDao {

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getFuelEntries(vehicleId: Long): Flow<List<FuelEntry>>

    @Query("SELECT AVG(mpg) FROM fuel_entries WHERE vehicleId = :vehicleId AND mpg > 0")
    fun getAverageMpg(vehicleId: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM fuel_entries WHERE vehicleId = :vehicleId")
    fun getFuelCount(vehicleId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelEntry(entry: FuelEntry): Long

    @Update
    suspend fun updateFuelEntry(entry: FuelEntry)

    @Delete
    suspend fun deleteFuelEntry(entry: FuelEntry)
}