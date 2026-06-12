package pl.complet.QuestSync.data.remote

import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WithingsResponse(
    @Json(name = "status") val status: Int,
    @Json(name = "body") val body: WithingsBody?
)

@JsonClass(generateAdapter = true)
data class WithingsBody(
    @Json(name = "measuregrps") val measureGroups: List<WithingsMeasureGroup>
)

@JsonClass(generateAdapter = true)
data class WithingsMeasureGroup(
    @Json(name = "date") val date: Long,
    @Json(name = "measures") val measures: List<WithingsMeasure>
)

@JsonClass(generateAdapter = true)
data class WithingsMeasure(
    @Json(name = "value") val value: Long,
    @Json(name = "type") val type: Int,
    @Json(name = "unit") val unit: Int
)

interface WithingsApi {
    @FormUrlEncoded
    @POST("measure")
    suspend fun getMeasures(
        @Field("action") action: String = "getmeas",
        @Field("access_token") accessToken: String
    ): WithingsResponse
}
