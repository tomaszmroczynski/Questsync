package pl.complet.QuestSync.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import pl.complet.QuestSync.R
import pl.complet.QuestSync.data.DataModule
import pl.complet.QuestSync.data.local.QuestActivityEntity
import pl.complet.QuestSync.data.repository.HealthRepository
import kotlin.math.sqrt

import android.util.Log

class QuestSnifferService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var repository: HealthRepository

    private var currentIntensity = 0.0
    private var totalCaloriesBurned = 0.0
    private var startTime = 0L

    companion object {
        const val CHANNEL_ID = "QuestSnifferChannel"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "QuestSnifferService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        repository = DataModule.provideHealthRepository(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        startTime = System.currentTimeMillis()
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("VR Activity Sniffer Active"))
        
        accelerometer?.let {
            val registered = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d(TAG, "Accelerometer registered: $registered")
        } ?: Log.e(TAG, "Linear Acceleration sensor not found!")

        startRealTimeSyncLoop()
    }

    private fun startRealTimeSyncLoop() {
        serviceScope.launch {
            Log.d(TAG, "Starting sync loop")
            while (isActive) {
                delay(5000) // Sync every 5 seconds
                
                val duration = (System.currentTimeMillis() - startTime) / 60000
                val activity = QuestActivityEntity(
                    durationMinutes = duration.toInt(),
                    caloriesBurned = totalCaloriesBurned.toInt(),
                    timestamp = System.currentTimeMillis(),
                    activityName = "Real-time VR Session"
                )
                
                Log.d(TAG, "Syncing real-time activity: ${activity.caloriesBurned} kcal")
                repository.saveQuestRealTimeData(activity)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            val magnitude = sqrt(x * x + y * y + z * z)
            currentIntensity = magnitude.toDouble()
            
            // Refined heuristic for Quest 3 movement:
            // Lower threshold to 0.5 to capture subtle head/arm movements.
            // Increased metabolic cost: standard VR games burn roughly 6-10 kcal/min.
            // 5 seconds sync loop means we target ~0.5 kcal per sync for moderate activity.
            if (magnitude > 0.5) {
                // magnitude usually ranges from 1.0 to 15.0 during active VR play.
                // At magnitude 5.0 (moderate), this adds ~0.02 kcal per sensor event.
                // Assuming ~50 events per second, this is ~1.0 kcal per second? No, too high.
                // SENSOR_DELAY_UI is roughly 60ms (~16Hz).
                // 16Hz * 0.002 * 5.0 magnitude = 0.16 kcal per second.
                // 0.16 * 60 = 9.6 kcal per minute. Perfect for intense VR (like Beat Saber).
                totalCaloriesBurned += (magnitude * 0.002)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VR Activity Sniffer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuestSync Sniffer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
    }
}
