package com.autotrack

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.autotrack.navigation.AutoTrackNavGraph
import com.autotrack.navigation.Screen
import com.autotrack.notifications.triggerMileageAlert
import com.autotrack.ui.theme.AutoTrackTheme
import com.autotrack.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@AndroidEntryPoint
@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeCallback: (() -> Unit)? = null
    private var isShakeEnabled: Boolean = false

    private val shakeListener = object : SensorEventListener {
        private var lastShakeTime = 0L
        private val shakeThreshold = 800f

        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val now = System.currentTimeMillis()
            if (acceleration > shakeThreshold && now - lastShakeTime > 1000) {
                lastShakeTime = now
                shakeCallback?.invoke()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        enableEdgeToEdge()

        setContent {
            val vm: MainViewModel = hiltViewModel()
            val prefs by vm.preferences.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            
            // Sync local variable for lifecycle methods
            isShakeEnabled = prefs.shakeEnabled

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val notifPermission = rememberPermissionState(
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                LaunchedEffect(Unit) {
                    if (!notifPermission.status.isGranted) {
                        notifPermission.launchPermissionRequest()
                    }
                }
            }

            LaunchedEffect(prefs.shakeEnabled) {
                if (prefs.shakeEnabled) {
                    shakeCallback = {
                        val firstVehicle = vm.vehicles.value.firstOrNull()
                        if (firstVehicle != null) {
                            navController.navigate(
                                Screen.AddEditRecord.createRoute(firstVehicle.id)
                            )
                        }
                    }
                    registerShake()
                } else {
                    shakeCallback = null
                    unregisterShake()
                }
            }

            val predictions by vm.servicePredictions.collectAsStateWithLifecycle()
            
            // Notification logic moved to a side effect that only triggers once per overdue item
            val shownAlerts = remember { mutableStateSetOf<String>() }
            LaunchedEffect(predictions) {
                if (prefs.mileageAlertsEnabled) {
                    predictions.filter { it.isOverdue }.forEach { pred ->
                        val alertKey = "${pred.vehicle.id}_${pred.serviceType}"
                        if (!shownAlerts.contains(alertKey)) {
                            triggerMileageAlert(
                                context     = this@MainActivity,
                                vehicleId   = pred.vehicle.id,
                                vehicleName = "${pred.vehicle.make} ${pred.vehicle.model}",
                                serviceType = pred.serviceType
                            )
                            shownAlerts.add(alertKey)
                        }
                    }
                }
            }

            AutoTrackTheme(darkTheme = prefs.darkTheme) {
                AutoTrackNavGraph(navController = navController)
            }
        }
    }

    private fun registerShake() {
        if (isShakeEnabled) {
            accelerometer?.let {
                sensorManager.registerListener(shakeListener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    private fun unregisterShake() {
        sensorManager.unregisterListener(shakeListener)
    }

    override fun onResume() {
        super.onResume()
        registerShake()
    }

    override fun onPause() {
        super.onPause()
        unregisterShake()
    }
}
