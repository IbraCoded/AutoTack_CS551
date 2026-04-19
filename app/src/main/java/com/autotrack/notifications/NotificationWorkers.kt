package com.autotrack.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.autotrack.MainActivity
import com.autotrack.R
import com.autotrack.data.local.repository.AutoTrackRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

const val CHANNEL_SERVICE  = "autotrack_service_reminders"
const val CHANNEL_MILEAGE  = "autotrack_mileage_alerts"
const val NOTIF_SERVICE_ID = 1001
const val NOTIF_MILEAGE_ID = 1002

@HiltWorker
class ServiceReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: AutoTrackRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        createChannels(applicationContext)
        val vehicles = repo.getAllVehicles().first()
        val records  = repo.getAllRecords().first()

        val now   = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val alerts = mutableListOf<String>()

        for (vehicle in vehicles) {
            val lastOil = records
                .filter { record -> record.vehicleId == vehicle.id && record.serviceType == "Oil Change" }
                .maxByOrNull { record -> record.date }
            if (lastOil != null) {
                val daysAgo = ((now - lastOil.date) / dayMs).toInt()
                if (daysAgo >= 350) alerts.add("${vehicle.make} ${vehicle.model}: Oil Change overdue")
            }
        }

        if (alerts.isNotEmpty()) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            val pi = PendingIntent.getActivity(
                applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            val notif = NotificationCompat.Builder(applicationContext, CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("AutoTrack — Services Due")
                .setStyle(NotificationCompat.BigTextStyle().bigText(alerts.joinToString("\n")))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_SERVICE_ID, notif)
        }
        return Result.success()
    }
}

@HiltWorker
class MileageAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: AutoTrackRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        createChannels(applicationContext)
        val vehicleId   = inputData.getLong("vehicleId", -1L)
        val serviceType = inputData.getString("serviceType") ?: return Result.success()
        val vehicleName = inputData.getString("vehicleName") ?: "Your vehicle"

        val logIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("navigate_to", "add_edit_record/$vehicleId")
        }
        val logPi = PendingIntent.getActivity(
            applicationContext, 1, logIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_MILEAGE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$vehicleName — Service Due")
            .setContentText("$serviceType is due based on your current mileage.")
            .addAction(0, "Log Now", logPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_MILEAGE_ID, notif)
        return Result.success()
    }
}

fun createChannels(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(
        NotificationChannel(CHANNEL_SERVICE, "Service Reminders",
            NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Periodic reminders for upcoming vehicle services"
        }
    )
    nm.createNotificationChannel(
        NotificationChannel(CHANNEL_MILEAGE, "Mileage Alerts",
            NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Context-aware alerts when a service is due by mileage"
        }
    )
}

fun scheduleServiceReminder(context: Context, intervalDays: Long = 7) {
    val request = PeriodicWorkRequestBuilder<ServiceReminderWorker>(intervalDays, TimeUnit.DAYS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "service_reminder",
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

fun triggerMileageAlert(context: Context, vehicleId: Long, vehicleName: String, serviceType: String) {
    val data = workDataOf(
        "vehicleId"   to vehicleId,
        "vehicleName" to vehicleName,
        "serviceType" to serviceType
    )
    val request = OneTimeWorkRequestBuilder<MileageAlertWorker>()
        .setInputData(data)
        .build()
    WorkManager.getInstance(context).enqueue(request)
}