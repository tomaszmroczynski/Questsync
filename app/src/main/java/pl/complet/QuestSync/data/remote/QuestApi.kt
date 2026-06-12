package pl.complet.QuestSync.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuestActivityResponse(
    @Json(name = "activities") val activities: List<QuestRemoteActivity>
)

@JsonClass(generateAdapter = true)
data class QuestRemoteActivity(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "duration_seconds") val durationSeconds: Int,
    @Json(name = "calories") val calories: Int,
    @Json(name = "timestamp") val timestamp: Long
)

interface QuestApi {
    @GET("activities")
    suspend fun getActivities(
        @Query("access_token") accessToken: String
    ): QuestActivityResponse
}
