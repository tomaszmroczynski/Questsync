package pl.complet.QuestSync.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
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

    private var sessionStartTime = 0L
    private var currentPackageName = "Unknown"
    private var currentFriendlyAppName = "VR Session"
    private var isSessionActive = false

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
        private const val TRACKING_INTERVAL_MS = 15000L // Check every 15s
        private const val SYNC_INTERVAL_MS = 30000L // Sync every 30s
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DECODER: Battery-Efficient Duration Tracking Initialized")
        repository = DataModule.provideHealthRepository(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Monitoring VR Activity..."))

        startTrackingLoop()
    }

    private fun startTrackingLoop() {
        serviceScope.launch {
            var lastSyncTime = System.currentTimeMillis()
            
            while (isActive) {
                detectForegroundApp()
                
                if (isSessionActive && System.currentTimeMillis() - lastSyncTime >= SYNC_INTERVAL_MS) {
                    syncCurrentSession("Live Update")
                    lastSyncTime = System.currentTimeMillis()
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
        } else {
            Log.w(TAG, "DECODER: No usage stats found. Check permissions.")
            handleReturnToHome()
        }
    }

    private fun handleAppSwitch(newPkg: String) {
        if (isSessionActive) {
            syncCurrentSession("Session Finalized")
        }
        
        currentPackageName = newPkg
        currentFriendlyAppName = appMap[newPkg] ?: newPkg.split(".").last().replaceFirstChar { it.uppercase() }
        sessionStartTime = System.currentTimeMillis()
        isSessionActive = true
        
        Log.i(TAG, "DECODER: Session Started -> $currentFriendlyAppName")
        updateNotification("Active Tracking: $currentFriendlyAppName")
    }

    private fun handleReturnToHome() {
        if (isSessionActive) {
            syncCurrentSession("Session Ended")
            isSessionActive = false
            currentPackageName = "Oculus Home"
            currentFriendlyAppName = "Oculus Home"
            Log.i(TAG, "DECODER: Returned to Home. Tracking Standby.")
            updateNotification("Monitoring VR Activity...")
        }
    }

    private fun syncCurrentSession(updateType: String) {
        val durationMs = System.currentTimeMillis() - sessionStartTime
        val durationMinutes = (durationMs / 60000).toInt().coerceAtLeast(1)
        
        val activity = QuestActivityEntity(
            durationMinutes = durationMinutes,
            caloriesBurned = 0, // Calories now calculated server-side based on duration
            timestamp = System.currentTimeMillis(),
            activityName = "$currentFriendlyAppName ($updateType)"
        )
        
        Log.d(TAG, "DECODER SYNC: $updateType | $currentFriendlyAppName | $durationMinutes min")
        serviceScope.launch {
            repository.saveQuestRealTimeData(activity, currentPackageName, true)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Smart VR Tracking", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuestSync Duration Tracker")
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
            syncCurrentSession("Service Stopped")
        }
        serviceScope.cancel()
        Log.d(TAG, "DECODER: Tracker Offline")
        super.onDestroy()
    }
}
