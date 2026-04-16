package com.autotrack.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.autotrack.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

// Channel IDs
const val CHANNEL_SERVICE  = "autotrack_service_reminders"
const val CHANNEL_MILEAGE  = "autotrack_mileage_alerts"
const val NOTIF_SERVICE_ID = 1001
const val NOTIF_MILEAGE_ID = 1002

@HiltWorker
class ServiceReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        createChannels(applicationContext)

        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPi = PendingIntent.getActivity(
            applicationContext, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val upcomingServices = inputData.getString("upcoming_services")
            ?: "Check the app for upcoming service reminders."

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(upcomingServices)
            .setBigContentTitle("AutoTrack — Services Due")
            .setSummaryText("Tap to review")

        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("AutoTrack — Check Your Services")
            .setContentText("You have upcoming service reminders.")
            .setStyle(bigTextStyle)
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_SERVICE_ID, notif)

        return Result.success()
    }
}

@HiltWorker
class MileageAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        createChannels(applicationContext)

        val vehicleId   = inputData.getLong("vehicleId", -1L)
        val serviceType = inputData.getString("serviceType") ?: return Result.success()
        val vehicleName = inputData.getString("vehicleName") ?: "Your vehicle"

        val logIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "add_edit_record/$vehicleId")
        }
        val logPi = PendingIntent.getActivity(
            applicationContext, 1, logIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .addLine("$serviceType is due")
            .addLine("Vehicle: $vehicleName")
            .setBigContentTitle("$vehicleName — Service Due")
            .setSummaryText("Tap to log the service")

        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_MILEAGE)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("$vehicleName — Service Due")
            .setContentText("$serviceType is due based on your mileage.")
            .setStyle(inboxStyle)
            .addAction(0, "Log Now", logPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val nm = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_MILEAGE_ID, notif)

        return Result.success()
    }
}

fun createChannels(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(
        NotificationChannel(
            CHANNEL_SERVICE,
            "Service Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Periodic reminders to check upcoming vehicle services"
        }
    )
    nm.createNotificationChannel(
        NotificationChannel(
            CHANNEL_MILEAGE,
            "Mileage Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when mileage reaches a service threshold"
        }
    )
}

fun scheduleServiceReminder(context: Context, intervalDays: Long = 7) {
    val request = PeriodicWorkRequestBuilder<ServiceReminderWorker>(
        intervalDays, TimeUnit.DAYS
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "service_reminder",
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

fun triggerMileageAlert(
    context: Context,
    vehicleId: Long,
    vehicleName: String,
    serviceType: String
) {
    val data = workDataOf(
        "vehicleId"   to vehicleId,
        "vehicleName" to vehicleName,
        "serviceType" to serviceType
    )
    WorkManager.getInstance(context).enqueue(
        OneTimeWorkRequestBuilder<MileageAlertWorker>()
            .setInputData(data)
            .build()
    )
}