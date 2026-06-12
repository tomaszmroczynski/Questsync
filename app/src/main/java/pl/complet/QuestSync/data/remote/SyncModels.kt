package pl.complet.QuestSync.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(
    val source: String,
    val type: String,
    val data: QuestSyncData
)

@Serializable
data class QuestSyncData(
    val activityName: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val timestamp: Long
)
