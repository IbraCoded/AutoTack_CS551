package com.autotrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.autotrack.data.local.dao.FuelEntryDao
import com.autotrack.data.local.dao.ServiceRecordDao
import com.autotrack.data.local.dao.VehicleDao
import com.autotrack.data.local.entity.FuelEntry
import com.autotrack.data.local.entity.ServiceRecord
import com.autotrack.data.local.entity.Vehicle

@Database(
    entities = [Vehicle::class, ServiceRecord::class, FuelEntry::class],
    version = 1,
    exportSchema = true
)
abstract class AutoTrackDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun fuelEntryDao(): FuelEntryDao
}