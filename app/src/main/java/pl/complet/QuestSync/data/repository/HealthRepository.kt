package pl.complet.QuestSync.data.repository

import kotlinx.coroutines.flow.Flow
import pl.complet.QuestSync.data.local.*
import pl.complet.QuestSync.data.remote.*

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import pl.complet.QuestSync.BuildConfig

import io.ktor.client.call.*

class HealthRepository(
    private val healthDao: HealthDao,
    private val ouraApi: OuraApi,
    private val withingsApi: WithingsApi,
    private val questApi: QuestApi
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    val questActivities: Flow<List<QuestActivityEntity>> = healthDao.getAllQuestActivities()
    val latestOuraMetrics: Flow<OuraMetricsEntity?> = healthDao.getLatestOuraMetrics()
    val latestWithingsMetrics: Flow<WithingsMetricsEntity?> = healthDao.getLatestWithingsMetrics()
    val latestSamsungMetrics: Flow<SamsungHealthMetricsEntity?> = healthDao.getLatestSamsungMetrics()

    suspend fun saveQuestRealTimeData(
        activity: QuestActivityEntity, 
        packageName: String? = null,
        isHeadsetWorn: Boolean = true
    ) {
        // Save locally
        healthDao.insertQuestActivity(activity)
        
        // Push to RipperMCP server
        if (BuildConfig.MCP_SERVER_URL.isNotEmpty()) {
            try {
                val mcpUrl = BuildConfig.MCP_SERVER_URL.trimEnd('/')
                val baseUrl = if (mcpUrl.endsWith("/mcp")) {
                    mcpUrl.substringBeforeLast("/mcp")
                } else {
                    mcpUrl
                }
                val syncUrl = "$baseUrl/sync/health-connect"
                
                android.util.Log.d("HealthRepository", "Pushing real-time data to: $syncUrl")
                
                val response = httpClient.post(syncUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(SyncRequest(
                        source = "quest",
                        type = "real-time-activity",
                        data = QuestSyncData(
                            activityName = activity.activityName,
                            packageName = packageName,
                            durationMinutes = activity.durationMinutes,
                            caloriesBurned = activity.caloriesBurned,
                            timestamp = activity.timestamp,
                            isHeadsetWorn = isHeadsetWorn
                        )
                    ))
                }
                android.util.Log.d("HealthRepository", "Server Response [${activity.activityName}]: ${response.status}")
            } catch (e: Exception) {
                android.util.Log.e("HealthRepository", "Sync Bridge Failed", e)
            }
        }
    }

    suspend fun syncQuestData(token: String) {
        val response = questApi.getActivities(token)
        response.activities.forEach { remote ->
            healthDao.insertQuestActivity(
                QuestActivityEntity(
                    durationMinutes = remote.durationSeconds / 60,
                    caloriesBurned = remote.calories,
                    timestamp = remote.timestamp,
                    activityName = remote.name
                )
            )
        }
    }

    suspend fun syncOuraData(token: String) {
        val response = ouraApi.getSleepData("Bearer $token")
        response.data.forEach { sleep ->
            healthDao.insertOuraMetrics(
                OuraMetricsEntity(
                    timestamp = System.currentTimeMillis(), // Placeholder for parsing actual timestamp
                    sleepDurationHours = sleep.durationSeconds / 3600.0,
                    readinessScore = sleep.readiness ?: 0,
                    averageHrv = sleep.hrv ?: 0.0
                )
            )
        }
    }

    suspend fun syncWithingsData(token: String) {
        val response = withingsApi.getMeasures(accessToken = token)
        response.body?.measureGroups?.forEach { group ->
            // Simple mapping for demonstration
            healthDao.insertWithingsMetrics(
                WithingsMetricsEntity(
                    timestamp = group.date * 1000,
                    weightKg = 70.0, // Placeholder
                    bodyFatPercentage = 20.0, // Placeholder
                    bloodPressureSystolic = 120,
                    bloodPressureDiastolic = 80
                )
            )
        }
    }
    
    suspend fun saveSamsungMetrics(metrics: SamsungHealthMetricsEntity) {
        healthDao.insertSamsungMetrics(metrics)
    }

    suspend fun refreshHistoryFromServer() {
        if (BuildConfig.MCP_SERVER_URL.isEmpty()) return
        
        val baseUrl = BuildConfig.MCP_SERVER_URL.trimEnd('/').substringBeforeLast("/mcp")
        val sources = listOf("quest", "oura", "samsung_health", "withings")
        
        sources.forEach { source ->
            try {
                val historyUrl = "$baseUrl/api/history/$source"
                val response: List<Float> = httpClient.get(historyUrl).body()
                
                // Convert simple numeric list back to entities for sparklines
                // Note: This is a simplified mapping to populate the UI trends
                when (source) {
                    "quest" -> {
                        val entities = response.mapIndexed { i, val_ ->
                            QuestActivityEntity(
                                id = i + 1000, // Offset to avoid collisions
                                durationMinutes = val_.toInt(),
                                caloriesBurned = 0,
                                timestamp = System.currentTimeMillis() - (response.size - i) * 86400000L,
                                activityName = "Historical Session"
                            )
                        }
                        healthDao.insertQuestActivities(entities)
                    }
                    "oura" -> {
                        val entities = response.mapIndexed { i, val_ ->
                            OuraMetricsEntity(
                                timestamp = System.currentTimeMillis() - (response.size - i) * 86400000L,
                                sleepDurationHours = 8.0,
                                readinessScore = val_.toInt(),
                                averageHrv = 50.0
                            )
                        }
                        healthDao.insertOuraMetricsList(entities)
                    }
                    "samsung_health" -> {
                        val entities = response.mapIndexed { i, val_ ->
                            SamsungHealthMetricsEntity(
                                timestamp = System.currentTimeMillis() - (response.size - i) * 86400000L,
                                stepCount = val_.toInt(),
                                activeMinutes = 30,
                                heartRateAverage = 70
                            )
                        }
                        healthDao.insertSamsungMetricsList(entities)
                    }
                    "withings" -> {
                        val entities = response.mapIndexed { i, val_ ->
                            WithingsMetricsEntity(
                                timestamp = System.currentTimeMillis() - (response.size - i) * 86400000L,
                                weightKg = val_.toDouble(),
                                bodyFatPercentage = 20.0,
                                bloodPressureSystolic = 120,
                                bloodPressureDiastolic = 80
                            )
                        }
                        healthDao.insertWithingsMetricsList(entities)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HealthRepository", "Failed to refresh history for $source", e)
            }
        }
    }

    fun getQuestTrend(days: Int): Flow<List<QuestActivityEntity>> {
        val since = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return healthDao.getQuestActivitiesSince(since)
    }

    fun getOuraTrend(days: Int): Flow<List<OuraMetricsEntity>> {
        val since = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return healthDao.getOuraMetricsSince(since)
    }

    fun getSamsungTrend(days: Int): Flow<List<SamsungHealthMetricsEntity>> {
        val since = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return healthDao.getSamsungMetricsSince(since)
    }

    fun getWithingsTrend(days: Int): Flow<List<WithingsMetricsEntity>> {
        val since = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return healthDao.getWithingsMetricsSince(since)
    }
}
