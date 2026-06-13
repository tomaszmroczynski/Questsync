package pl.complet.QuestSync.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import pl.complet.QuestSync.data.DataModule
import pl.complet.QuestSync.data.local.QuestActivityEntity
import pl.complet.QuestSync.data.repository.HealthRepository
import java.util.Locale

class QuestSnifferService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: HealthRepository
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var sensorManager: SensorManager
    private lateinit var powerManager: PowerManager
    private var proximitySensor: Sensor? = null

    private var sessionStartTime = 0L
    private var cumulativePausedTime = 0L
    private var lastPauseTimestamp = 0L
    
    private var currentPackageName = "Unknown"
    private var currentFriendlyAppName = "VR Session"
    private var isSessionActive = false
    
    private var isHeadsetWorn = false
    private var lastRemovedTimestamp = 0L

    private val appMap = mapOf(
        "com.beatgames.beatsaber" to "Beat Saber",
        "com.cloudheadgames.pistolwhip" to "Pistol Whip",
        "com.synthriders.quest" to "Synth Riders",
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
        private const val TRACKING_INTERVAL_MS = 15000L
        private const val SYNC_INTERVAL_MS = 30000L
        private const val AUTO_FINALIZE_TIMEOUT_MS = 60000L // 60s removal = end session
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DECODER: Initializing Truthful Headset Tracking v2")
        repository = DataModule.provideHealthRepository(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Monitoring VR Activity..."))

        startTrackingLoop()
    }

    private fun startTrackingLoop() {
        serviceScope.launch {
            var lastSyncTime = System.currentTimeMillis()
            
            while (isActive) {
                val isInteractive = powerManager.isInteractive
                
                // Effective Worn State: Proximity says Near AND Screen is ON
                val effectivelyWorn = isHeadsetWorn && isInteractive
                
                if (effectivelyWorn) {
                    if (lastPauseTimestamp != 0L) {
                        // Resumed tracking
                        cumulativePausedTime += (System.currentTimeMillis() - lastPauseTimestamp)
                        lastPauseTimestamp = 0L
                        Log.d(TAG, "DECODER: Tracking Resumed")
                    }
                    
                    detectForegroundApp()
                    
                    if (isSessionActive && System.currentTimeMillis() - lastSyncTime >= SYNC_INTERVAL_MS) {
                        syncCurrentSession("Live Update", true)
                        lastSyncTime = System.currentTimeMillis()
                    }
                } else {
                    if (lastPauseTimestamp == 0L) {
                        lastPauseTimestamp = System.currentTimeMillis()
                        Log.d(TAG, "DECODER: Tracking Paused (effectivelyWorn=false, isWorn=$isHeadsetWorn, isInteractive=$isInteractive)")
                        // Send one final packet to server indicating pause
                        if (isSessionActive) {
                            syncCurrentSession("Paused", false)
                        }
                    }
                    
                    // Auto-finalize logic: if removed for too long, reset session
                    if (isSessionActive && System.currentTimeMillis() - lastRemovedTimestamp > AUTO_FINALIZE_TIMEOUT_MS && !isHeadsetWorn) {
                        Log.i(TAG, "DECODER: Auto-finalizing session due to prolonged inactivity")
                        handleReturnToHome()
                    }
                }
                
                delay(TRACKING_INTERVAL_MS)
            }
        }
    }

    private fun detectForegroundApp() {
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 300000, time)
        
        if (!stats.isNullOrEmpty()) {
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            val activeApp = sortedStats.firstOrNull { stat ->
                val pkg = stat.packageName
                pkg != "com.oculus.vrshell" && 
                pkg != "com.oculus.shell" && 
                pkg != "android" && 
                pkg != "com.android.settings" &&
                pkg != packageName 
            }

            activeApp?.packageName?.let { pkg ->
                if (pkg != currentPackageName) {
                    handleAppSwitch(pkg)
                }
            } ?: handleReturnToHome()
        }
    }

    private fun handleAppSwitch(newPkg: String) {
        if (isSessionActive) {
            syncCurrentSession("Session Finalized", isHeadsetWorn && powerManager.isInteractive)
        }
        
        currentPackageName = newPkg
        currentFriendlyAppName = appMap[newPkg] ?: newPkg.split(".").last().replaceFirstChar { it.uppercase() }
        sessionStartTime = System.currentTimeMillis()
        cumulativePausedTime = 0L
        lastPauseTimestamp = 0L
        isSessionActive = true
        
        Log.i(TAG, "DECODER: Session Started -> $currentFriendlyAppName")
        updateNotification("Active Tracking: $currentFriendlyAppName")
    }

    private fun handleReturnToHome() {
        if (isSessionActive) {
            syncCurrentSession("Session Ended", false)
            isSessionActive = false
            currentPackageName = "Oculus Home"
            currentFriendlyAppName = "Oculus Home"
            Log.i(TAG, "DECODER: Session Terminated. Standby.")
            updateNotification("Monitoring VR Activity...")
        }
    }

    private fun syncCurrentSession(updateType: String, effectivelyWorn: Boolean) {
        // Duration = Total Time - Time while headset was off
        val now = System.currentTimeMillis()
        val currentPauseEffect = if (lastPauseTimestamp != 0L) (now - lastPauseTimestamp) else 0L
        val activeDurationMs = (now - sessionStartTime) - cumulativePausedTime - currentPauseEffect
        
        val durationMinutes = (activeDurationMs / 60000).toInt().coerceAtLeast(0)
        
        val activity = QuestActivityEntity(
            durationMinutes = durationMinutes,
            caloriesBurned = 0,
            timestamp = now,
            activityName = "$currentFriendlyAppName ($updateType)"
        )
        
        Log.d(TAG, "DECODER SENDING: $currentFriendlyAppName | Worn: $effectivelyWorn | Duration: $durationMinutes min")
        serviceScope.launch {
            repository.saveQuestRealTimeData(activity, currentPackageName, effectivelyWorn)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val rawValue = event.values[0]
            val maxRange = event.sensor.maximumRange
            val worn = rawValue < maxRange || rawValue == 0f
            
            if (worn != isHeadsetWorn) {
                isHeadsetWorn = worn
                if (!worn) lastRemovedTimestamp = System.currentTimeMillis()
                Log.d(TAG, "DECODER: Proximity Sensor State -> Worn: $worn")
            }
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
            .setContentTitle("QuestSync Truthful Tracker")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
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
        if (isSessionActive) {
            syncCurrentSession("Service Stopped", false)
        }
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        super.onDestroy()
    }
}
