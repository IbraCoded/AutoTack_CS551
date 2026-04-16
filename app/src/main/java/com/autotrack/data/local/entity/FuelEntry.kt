package com.autotrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuel_entries",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class FuelEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: Long,
    val litresFilled: Double,
    val costPerLitre: Double,
    val totalCost: Double,
    val mileageAtFill: Int,
    val fuelType: String = "Petrol",
    val isFullTank: Boolean = true,
    val notes: String = "",
    val mpg: Double = 0.0
)