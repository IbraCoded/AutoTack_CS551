package com.autotrack.data.local.dao

import androidx.room.*
import com.autotrack.data.local.entity.ServiceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRecordDao {

    @Query("SELECT * FROM service_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getRecordsForVehicle(vehicleId: Long): Flow<List<ServiceRecord>>

    @Query("SELECT SUM(cost) FROM service_records WHERE vehicleId = :vehicleId")
    fun getTotalSpend(vehicleId: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM service_records WHERE vehicleId = :vehicleId")
    fun getRecordCount(vehicleId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ServiceRecord): Long

    @Update
    suspend fun updateRecord(record: ServiceRecord)

    @Delete
    suspend fun deleteRecord(record: ServiceRecord)
}