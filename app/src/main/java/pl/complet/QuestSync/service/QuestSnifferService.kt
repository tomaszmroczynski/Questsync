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
        "com.samsung.android.app.shealth" to "Samsung Health",
        "com.fyian.TheThrillOfTheFight" to "Thrill of the Fight"
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
        Log.d(TAG, "DECODER: Initializing Smart Tracker with High Frequency...")
        repository = DataModule.provideHealthRepository(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        
        startTime = System.currentTimeMillis()
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Smart VR Tracking Active"))
        
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "DECODER: Proximity listener active (Range: ${it.maximumRange})")
        } ?: Log.e(TAG, "DECODER: Proximity sensor NOT FOUND")

        // Force wear state if proximity is missing to ensure accelerometer starts
        if (proximitySensor == null) {
            Log.w(TAG, "DECODER: Proximity sensor missing - forcing worn state")
            isHeadsetWorn = true
            registerAccelerometer()
        }

        startAdaptiveSyncLoop()
    }

    private fun registerAccelerometer() {
        accelerometer?.let {
            // Using SENSOR_DELAY_GAME for higher frequency polling
            val success = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "DECODER: Accelerometer registered ($success) at GAME frequency")
        }
    }

    private fun startAdaptiveSyncLoop() {
        serviceScope.launch {
            while (isActive) {
                checkUsagePermission()
                
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
                    
                    Log.d(TAG, "DECODER SYNC: $currentFriendlyAppName | Cal: ${activity.caloriesBurned} | Pkg: $currentPackageName")
                    repository.saveQuestRealTimeData(activity, currentPackageName, isHeadsetWorn)
                }
            }
        }
    }

    private fun checkUsagePermission() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        if (mode != AppOpsManager.MODE_ALLOWED) {
            Log.e(TAG, "DECODER CRITICAL: Usage Access Permission is NOT GRANTED!")
        } else {
            Log.d(TAG, "DECODER: Usage Access is verified")
        }
    }

    private fun detectForegroundApp() {
        val time = System.currentTimeMillis()
        // Query last 5 minutes to ensure we don't miss anything due to time drift
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 300000, time)
        if (!stats.isNullOrEmpty()) {
            // Sort by lastTimeUsed to get the actual current apps
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            
            // Print top 5 apps for debugging
            Log.d(TAG, "DECODER: Top 5 Active Apps:")
            sortedStats.take(5).forEach { stat ->
                Log.d(TAG, "  - ${stat.packageName} (Last used: ${stat.lastTimeUsed})")
            }

            // Find the first app that isn't a known system shell/home
            val activeApp = sortedStats.firstOrNull { stat ->
                val pkg = stat.packageName
                pkg != "com.oculus.vrshell" && 
                pkg != "com.oculus.shell" && 
                pkg != "android" && 
                pkg != "com.android.settings" &&
                pkg != packageName // Ignore QuestSync itself
            }

            activeApp?.packageName?.let { pkg ->
                if (pkg != currentPackageName) {
                    currentPackageName = pkg
                    currentFriendlyAppName = appMap[pkg] ?: pkg.split(".").last().replaceFirstChar { it.uppercase() }
                    Log.i(TAG, "DECODER: App Focus shifted to -> $currentFriendlyAppName ($pkg)")
                    updateNotification("Tracking: $currentFriendlyAppName")
                }
            }
        } else {
            Log.w(TAG, "DECODER: UsageStatsManager returned NO stats. Permission might be blocked.")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_PROXIMITY -> {
                val rawValue = event.values[0]
                val worn = rawValue < 1.0f || rawValue <= (event.sensor.maximumRange * 0.5f)
                if (worn != isHeadsetWorn) {
                    isHeadsetWorn = worn
                    handleHeadsetStateChange(worn)
                }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                
                if (magnitude > IDLE_THRESHOLD) {
                    lastMovementTime = System.currentTimeMillis()
                }

                // Calorie Boost: multiplier increased from 0.002 to 0.01 for visibility
                if (magnitude > 0.4) {
                    totalCaloriesBurned += (magnitude * 0.01)
                }
            }
        }
    }

    private fun handleHeadsetStateChange(worn: Boolean) {
        Log.i(TAG, "DECODER: Headset worn state changed: $worn")
        if (worn) {
            registerAccelerometer()
            updateNotification("Active VR Tracking...")
        } else {
            sensorManager.unregisterListener(this, accelerometer)
            Log.d(TAG, "DECODER: Sensors paused (Power Save)")
            updateNotification("Headset standby...")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Smart VR Tracking", NotificationManager.IMPORTANCE_LOW)
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
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, createNotification(text))
        } catch (e: Exception) {
            Log.e(TAG, "DECODER: Notification update failed", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        Log.d(TAG, "DECODER: Tracking Link Severed")
    }
}
