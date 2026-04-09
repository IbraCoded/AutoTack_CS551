package com.autotrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val make: String,
    val model: String,
    val year: Int,
    val mileage: Int,
    val nickname: String = "",
    val colour: String = "",
    val fuelType: String = "Petrol",
    val photoUri: String = ""
)