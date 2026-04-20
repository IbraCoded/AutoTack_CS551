package com.autotrack.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

fun createImageUri(context: Context): Uri {
    val imageFile = File(
        context.externalCacheDir,
        "photo_${System.currentTimeMillis()}.jpg"
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

fun fetchLocation(
    context: Context,
    onResult: (String) -> Unit
) {
    val fusedClient = com.google.android.gms.location.LocationServices
        .getFusedLocationProviderClient(context)
    try {
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = android.location.Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(
                    location.latitude, location.longitude, 1
                )
                val address = addresses?.firstOrNull()
                val result  = listOfNotNull(
                    address?.featureName,
                    address?.locality,
                    address?.adminArea
                ).joinToString(", ")
                onResult(result.ifBlank { "Unknown location" })
            } else {
                onResult("")
            }
        }
    } catch (e: SecurityException) {
        onResult("")
    }
}