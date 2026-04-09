package com.autotrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_records",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val serviceType: String,
    val date: Long,           // epoch millis
    val mileage: Int = 0,
    val cost: Double = 0.0,
    val garage: String = "",
    val notes: String = "",
    val receiptPhotoUri: String = "",
    val nextServiceDate: Long? = null,
    val nextServiceMileage: Int? = null
)