package com.autotrack.data.local.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "autotrack_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DISTANCE_UNIT     = stringPreferencesKey("distance_unit")
        val CURRENCY          = stringPreferencesKey("currency")
        val DARK_THEME        = booleanPreferencesKey("dark_theme")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_INTERVAL = stringPreferencesKey("reminder_interval")
        val MILEAGE_ALERTS    = booleanPreferencesKey("mileage_alerts")
        val OVERDUE_THRESHOLD = intPreferencesKey("overdue_threshold")
        val SHAKE_ENABLED     = booleanPreferencesKey("shake_enabled")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            AppPreferences(
                distanceUnit         = prefs[Keys.DISTANCE_UNIT]     ?: "mi",
                currency             = prefs[Keys.CURRENCY]          ?: "GBP £",
                darkTheme            = prefs[Keys.DARK_THEME]        ?: false,
                remindersEnabled     = prefs[Keys.REMINDERS_ENABLED] ?: true,
                reminderInterval     = prefs[Keys.REMINDER_INTERVAL] ?: "Weekly",
                mileageAlertsEnabled = prefs[Keys.MILEAGE_ALERTS]    ?: true,
                overdueThresholdDays = prefs[Keys.OVERDUE_THRESHOLD] ?: 7,
                shakeEnabled         = prefs[Keys.SHAKE_ENABLED]     ?: true
            )
        }

    suspend fun updateDistanceUnit(unit: String)         = context.dataStore.edit { it[Keys.DISTANCE_UNIT]     = unit }
    suspend fun updateCurrency(currency: String)          = context.dataStore.edit { it[Keys.CURRENCY]          = currency }
    suspend fun updateDarkTheme(enabled: Boolean)         = context.dataStore.edit { it[Keys.DARK_THEME]        = enabled }
    suspend fun updateRemindersEnabled(enabled: Boolean)  = context.dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    suspend fun updateReminderInterval(interval: String)  = context.dataStore.edit { it[Keys.REMINDER_INTERVAL] = interval }
    suspend fun updateMileageAlerts(enabled: Boolean)     = context.dataStore.edit { it[Keys.MILEAGE_ALERTS]    = enabled }
    suspend fun updateOverdueThreshold(days: Int)         = context.dataStore.edit { it[Keys.OVERDUE_THRESHOLD] = days }
    suspend fun updateShakeEnabled(enabled: Boolean)      = context.dataStore.edit { it[Keys.SHAKE_ENABLED]     = enabled }
}