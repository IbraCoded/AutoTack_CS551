package com.autotrack.data.local.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val DARK_THEME          = booleanPreferencesKey("dark_theme")
        val DISTANCE_UNIT       = stringPreferencesKey("distance_unit")
        val CURRENCY            = stringPreferencesKey("currency")
        val REMINDERS_ENABLED   = booleanPreferencesKey("reminders_enabled")
        val REMINDER_INTERVAL   = stringPreferencesKey("reminder_interval")
        val MILEAGE_ALERTS      = booleanPreferencesKey("mileage_alerts")
        val OVERDUE_THRESHOLD   = intPreferencesKey("overdue_threshold")
    }

    val preferences: Flow<AppPreferences> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            AppPreferences(
                darkTheme            = prefs[Keys.DARK_THEME]         ?: false,
                distanceUnit         = prefs[Keys.DISTANCE_UNIT]      ?: "mi",
                currency             = prefs[Keys.CURRENCY]           ?: "GBP £",
                remindersEnabled     = prefs[Keys.REMINDERS_ENABLED]  ?: true,
                reminderInterval     = prefs[Keys.REMINDER_INTERVAL]  ?: "Weekly",
                mileageAlertsEnabled = prefs[Keys.MILEAGE_ALERTS]     ?: true,
                overdueThresholdDays = prefs[Keys.OVERDUE_THRESHOLD]  ?: 7
            )
        }

    suspend fun updateDarkTheme(value: Boolean) =
        dataStore.edit { it[Keys.DARK_THEME] = value }
    suspend fun updateDistanceUnit(value: String) =
        dataStore.edit { it[Keys.DISTANCE_UNIT] = value }
    suspend fun updateCurrency(value: String) =
        dataStore.edit { it[Keys.CURRENCY] = value }
    suspend fun updateRemindersEnabled(value: Boolean) =
        dataStore.edit { it[Keys.REMINDERS_ENABLED] = value }
    suspend fun updateReminderInterval(value: String) =
        dataStore.edit { it[Keys.REMINDER_INTERVAL] = value }
    suspend fun updateMileageAlerts(value: Boolean) =
        dataStore.edit { it[Keys.MILEAGE_ALERTS] = value }
    suspend fun updateOverdueThreshold(value: Int) =
        dataStore.edit { it[Keys.OVERDUE_THRESHOLD] = value }
}