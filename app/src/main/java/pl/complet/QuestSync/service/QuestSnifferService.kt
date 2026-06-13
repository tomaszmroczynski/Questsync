package pl.complet.QuestSync.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.input.InputManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.InputDevice
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import pl.complet.QuestSync.data.DataModule
import pl.complet.QuestSync.data.local.QuestActivityEntity
import pl.complet.QuestSync.data.repository.HealthRepository
import java.util.Locale

class QuestSnifferService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: HealthRepository
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var powerManager: PowerManager
    private lateinit var inputManager: InputManager

    private var sessionStartTime = 0L
    private var cumulativePausedTime = 0L
    private var lastPauseTimestamp = 0L
    
    private var currentPackageName = "Unknown"
    private var currentFriendlyAppName = "VR Session"
    private var isSessionActive = false
    
    private var isLeftControllerConnected = false
    private var isRightControllerConnected = false
    private var lastControllersActiveTimestamp = 0L

    private val appMap = mapOf(
        "com.beatgames.beatsaber" to "Beat Saber",
        "com.cloudheadgames.pistolwhip" to "Pistol Whip",
        "com.synthriders.quest" to "Synth Riders",
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
        private const val AUTO_FINALIZE_TIMEOUT_MS = 60000L 
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DECODER: Initializing Controller-Based Tracking")
        repository = DataModule.provideHealthRepository(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Monitoring VR Activity..."))

        startTrackingLoop()
    }

    private fun startTrackingLoop() {
        serviceScope.launch {
            var lastSyncTime = System.currentTimeMillis()
            
            while (isActive) {
                checkControllers()
                val isInteractive = powerManager.isInteractive
                
                // Effective Worn State: Both controllers connected AND Screen is ON
                val effectivelyActive = isLeftControllerConnected && isRightControllerConnected && isInteractive
                
                if (effectivelyActive) {
                    if (lastPauseTimestamp != 0L) {
                        cumulativePausedTime += (System.currentTimeMillis() - lastPauseTimestamp)
                        lastPauseTimestamp = 0L
                        Log.d(TAG, "DECODER: Tracking Resumed (Controllers Linked)")
                    }
                    
                    detectForegroundApp()
                    
                    if (isSessionActive && System.currentTimeMillis() - lastSyncTime >= SYNC_INTERVAL_MS) {
                        syncCurrentSession("Live Update", true)
                        lastSyncTime = System.currentTimeMillis()
                    }
                    lastControllersActiveTimestamp = System.currentTimeMillis()
                } else {
                    if (lastPauseTimestamp == 0L) {
                        lastPauseTimestamp = System.currentTimeMillis()
                        Log.d(TAG, "DECODER: Tracking Paused (Missing Controllers or Sleep)")
                        if (isSessionActive) {
                            syncCurrentSession("Paused", false)
                        }
                    }
                    
                    if (isSessionActive && System.currentTimeMillis() - lastControllersActiveTimestamp > AUTO_FINALIZE_TIMEOUT_MS) {
                        Log.i(TAG, "DECODER: Auto-finalizing session due to controller disconnect")
                        handleReturnToHome()
                    }
                }
                
                delay(TRACKING_INTERVAL_MS)
            }
        }
    }

    private fun checkControllers() {
        var left = false
        var right = false
        
        val deviceIds = inputManager.inputDeviceIds
        for (id in deviceIds) {
            val device = inputManager.getInputDevice(id) ?: continue
            val name = device.name.lowercase()
            // Generic detection for Quest controllers
            if (name.contains("oculus") || name.contains("meta") || name.contains("controller")) {
                if (name.contains("left")) left = true
                if (name.contains("right")) right = true
            }
        }
        
        isLeftControllerConnected = left
        isRightControllerConnected = right
        Log.d(TAG, "DECODER: Controllers connected: Left=$isLeftControllerConnected, Right=$isRightControllerConnected")
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
            syncCurrentSession("Session Finalized", isLeftControllerConnected && isRightControllerConnected && powerManager.isInteractive)
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
        
        Log.d(TAG, "DECODER SENDING: $currentFriendlyAppName | Active: $durationMinutes min")
        serviceScope.launch {
            repository.saveQuestRealTimeData(activity, currentPackageName, effectivelyWorn)
        }
    }

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
        serviceScope.cancel()
        super.onDestroy()
    }
}
