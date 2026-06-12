package pl.complet.QuestSync.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quest_activity")
data class QuestActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val timestamp: Long,
    val activityName: String
)

@Entity(tableName = "oura_metrics")
data class OuraMetricsEntity(
    @PrimaryKey val timestamp: Long,
    val sleepDurationHours: Double,
    val readinessScore: Int,
    val averageHrv: Double
)

@Entity(tableName = "withings_metrics")
data class WithingsMetricsEntity(
    @PrimaryKey val timestamp: Long,
    val weightKg: Double,
    val bodyFatPercentage: Double,
    val bloodPressureSystolic: Int,
    val bloodPressureDiastolic: Int
)

@Entity(tableName = "samsung_health_metrics")
data class SamsungHealthMetricsEntity(
    @PrimaryKey val timestamp: Long,
    val stepCount: Int,
    val activeMinutes: Int,
    val heartRateAverage: Int
)
