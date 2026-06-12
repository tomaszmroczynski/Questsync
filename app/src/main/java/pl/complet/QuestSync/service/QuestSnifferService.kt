package pl.complet.QuestSync.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import pl.complet.QuestSync.data.DataModule
import pl.complet.QuestSync.data.local.QuestActivityEntity
import pl.complet.QuestSync.data.repository.HealthRepository
import java.util.Locale
import kotlin.math.sqrt

class QuestSnifferService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null
    private lateinit var repository: HealthRepository
    private lateinit var usageStatsManager: UsageStatsManager

    private var totalCaloriesBurned = 0.0
    private var startTime = 0L
    private var isHeadsetWorn = false
    private var lastMovementTime = System.currentTimeMillis()
    private var currentPackageName = "Unknown"
    private var currentFriendlyAppName = "VR Session"

    private val appMap = mapOf(
        "com.beatgames.beatsaber" to "Beat Saber",
        "com.superhotgame.superhot" to "Superhot VR",
        "com.fitxr.fitxr" to "FitXR",
        "com.oculus.vrshell" to "Oculus Home",
        "com.funnyvg.totaleasy" to "Les Mills Bodycombat",
        "com.welltory.welltory" to "Welltory",
        "com.samsung.android.app.shealth" to "Samsung Health"
    )

    companion object {
        const val CHANNEL_ID = "QuestSnifferChannel"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "QuestSnifferService"
        private const val IDLE_THRESHOLD = 0.1
        private const val IDLE_TIMEOUT_MS = 120000L // 2 minutes
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created with Smart Detection")
        repository = DataModule.provideHealthRepository(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        
        startTime = System.currentTimeMillis()
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Awaiting Headset Wear..."))
        
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: Log.e(TAG, "Proximity sensor not found!")

        startAdaptiveSyncLoop()
    }

    private fun startAdaptiveSyncLoop() {
        serviceScope.launch {
            while (isActive) {
                val isIdle = (System.currentTimeMillis() - lastMovementTime) > IDLE_TIMEOUT_MS
                val delayTime = if (!isHeadsetWorn) 30000L else if (isIdle) 60000L else 5000L
                
                delay(delayTime)
                
                if (isHeadsetWorn) {
                    detectForegroundApp()
                    
                    val duration = (System.currentTimeMillis() - startTime) / 60000
                    val activity = QuestActivityEntity(
                        durationMinutes = duration.toInt(),
                        caloriesBurned = totalCaloriesBurned.toInt(),
                        timestamp = System.currentTimeMillis(),
                        activityName = currentFriendlyAppName
                    )
                    
                    Log.d(TAG, "Syncing: $currentFriendlyAppName | Cal: ${activity.caloriesBurned} | Idle: $isIdle")
                    repository.saveQuestRealTimeData(activity, currentPackageName, isHeadsetWorn)
                } else {
                    Log.d(TAG, "Headset not worn - skip high-freq sync")
                }
            }
        }
    }

    private fun detectForegroundApp() {
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time)
        if (stats != null && stats.isNotEmpty()) {
            val lastApp = stats.maxByOrNull { it.lastTimeUsed }
            lastApp?.packageName?.let { pkg ->
                if (pkg != currentPackageName) {
                    currentPackageName = pkg
                    currentFriendlyAppName = appMap[pkg] ?: pkg.split(".").last().replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                    }
                    Log.d(TAG, "New app detected: $currentFriendlyAppName")
                    updateNotification("Tracking: $currentFriendlyAppName")
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                val worn = distance < (proximitySensor?.maximumRange ?: 1.0f)
                if (worn != isHeadsetWorn) {
                    isHeadsetWorn = worn
                    handleHeadsetStateChange(worn)
                }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val magnitude = sqrt(
                    event.values[0] * event.values[0] + 
                    event.values[1] * event.values[1] + 
                    event.values[2] * event.values[2]
                )
                
                if (magnitude > IDLE_THRESHOLD) {
                    lastMovementTime = System.currentTimeMillis()
                }

                if (magnitude > 0.5) {
                    totalCaloriesBurned += (magnitude * 0.002)
                }
            }
        }
    }

    private fun handleHeadsetStateChange(worn: Boolean) {
        Log.d(TAG, "Headset Worn: $worn")
        if (worn) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            updateNotification("Active VR Tracking...")
        } else {
            sensorManager.unregisterListener(this, accelerometer)
            updateNotification("Headset standby...")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Smart VR Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuestSync Smart Tracker")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        try {
            val notification = createNotification(text)
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission denied", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
    }
}
