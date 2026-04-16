package com.autotrack.data.local.repository


data class AppPreferences(
    val darkTheme: Boolean = false,
    val distanceUnit: String = "mi",
    val currency: String = "GBP £",
    val remindersEnabled: Boolean = true,
    val reminderInterval: String = "Weekly",
    val mileageAlertsEnabled: Boolean = true,
    val overdueThresholdDays: Int = 7
)