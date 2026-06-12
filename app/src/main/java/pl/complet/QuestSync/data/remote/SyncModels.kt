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
    val packageName: String? = null,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val timestamp: Long,
    val isHeadsetWorn: Boolean = true
)
