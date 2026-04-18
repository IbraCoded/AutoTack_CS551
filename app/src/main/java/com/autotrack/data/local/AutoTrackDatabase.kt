package com.autotrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.autotrack.data.local.dao.FuelEntryDao
import com.autotrack.data.local.dao.ServiceRecordDao
import com.autotrack.data.local.dao.VehicleDao
import com.autotrack.data.local.entity.FuelEntry
import com.autotrack.data.local.entity.ServiceRecord
import com.autotrack.data.local.entity.Vehicle

@Database(
    entities = [Vehicle::class, ServiceRecord::class, FuelEntry::class],
    version = 2,
    exportSchema = false
)
abstract class AutoTrackDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun fuelEntryDao(): FuelEntryDao

    companion object {
        @Volatile private var INSTANCE: AutoTrackDatabase? = null

        fun getDatabase(context: Context): AutoTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AutoTrackDatabase::class.java,
                    "autotrack_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}