package pl.complet.QuestSync.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OuraResponse(
    @Json(name = "data") val data: List<OuraSleepData>
)

@JsonClass(generateAdapter = true)
data class OuraSleepData(
    @Json(name = "total_sleep_duration") val durationSeconds: Int,
    @Json(name = "readiness_score") val readiness: Int?,
    @Json(name = "average_hrv") val hrv: Double?,
    @Json(name = "timestamp") val timestamp: String
)

interface OuraApi {
    @GET("usercollection/sleep")
    suspend fun getSleepData(
        @Header("Authorization") token: String
    ): OuraResponse
}
