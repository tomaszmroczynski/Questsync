package pl.complet.QuestSync.data

import kotlinx.coroutines.flow.first
import pl.complet.QuestSync.data.local.QuestActivityEntity
import pl.complet.QuestSync.data.repository.HealthRepository
import java.text.SimpleDateFormat
import java.util.*

class AIAggregator(private val repository: HealthRepository) {

    suspend fun getAggregatedDataString(): String {
        val questActivities = repository.questActivities.first()
        val latestOura = repository.latestOuraMetrics.first()
        val latestWithings = repository.latestWithingsMetrics.first()
        val latestSamsung = repository.latestSamsungMetrics.first()

        val sb = StringBuilder()
        sb.append("Health and Activity Data Summary (Generated on ${getCurrentDate()}):\n\n")

        sb.append("--- Meta Quest VR Activities ---\n")
        if (questActivities.isEmpty()) {
            sb.append("No VR activities recorded.\n")
        } else {
            questActivities.take(5).forEach { activity ->
                sb.append("- ${activity.activityName}: ${activity.durationMinutes} min, ${activity.caloriesBurned} kcal (${formatTimestamp(activity.timestamp)})\n")
            }
        }
        sb.append("\n")

        sb.append("--- Wearable Metrics ---\n")
        latestOura?.let {
            sb.append("Oura: Sleep ${String.format("%.1f", it.sleepDurationHours)}h, Readiness ${it.readinessScore}, Avg HRV ${it.averageHrv}\n")
        } ?: sb.append("Oura: No data available\n")

        latestWithings?.let {
            sb.append("Withings: Weight ${it.weightKg}kg, Body Fat ${it.bodyFatPercentage}%, BP ${it.bloodPressureSystolic}/${it.bloodPressureDiastolic}\n")
        } ?: sb.append("Withings: No data available\n")

        latestSamsung?.let {
            sb.append("Samsung Health: Steps ${it.stepCount}, Active ${it.activeMinutes}m, Avg HR ${it.heartRateAverage}bpm\n")
        } ?: sb.append("Samsung Health: No data available\n")

        return sb.toString()
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
