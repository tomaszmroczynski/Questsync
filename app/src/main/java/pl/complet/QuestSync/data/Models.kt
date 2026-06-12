package pl.complet.QuestSync.data

import kotlinx.serialization.Serializable

@Serializable
data class QuestActivity(
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val timestamp: Long,
    val activityName: String
)

@Serializable
data class OuraMetrics(
    val sleepDurationHours: Double,
    val readinessScore: Int,
    val averageHrv: Double,
    val timestamp: Long
)

@Serializable
data class WithingsMetrics(
    val weightKg: Double,
    val bodyFatPercentage: Double,
    val bloodPressureSystolic: Int,
    val bloodPressureDiastolic: Int,
    val timestamp: Long
)

@Serializable
data class SamsungHealthMetrics(
    val stepCount: Int,
    val activeMinutes: Int,
    val heartRateAverage: Int,
    val timestamp: Long
)

@Serializable
data class AggregatedHealthData(
    val questActivity: List<QuestActivity> = emptyList(),
    val ouraMetrics: OuraMetrics? = null,
    val withingsMetrics: WithingsMetrics? = null,
    val samsungHealthMetrics: SamsungHealthMetrics? = null,
    val summary: String? = null
)
