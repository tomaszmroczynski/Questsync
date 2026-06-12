package pl.complet.QuestSync.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestActivity(activity: QuestActivityEntity)

    @Query("SELECT * FROM quest_activity ORDER BY timestamp DESC")
    fun getAllQuestActivities(): Flow<List<QuestActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOuraMetrics(metrics: OuraMetricsEntity)

    @Query("SELECT * FROM oura_metrics ORDER BY timestamp DESC LIMIT 1")
    fun getLatestOuraMetrics(): Flow<OuraMetricsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithingsMetrics(metrics: WithingsMetricsEntity)

    @Query("SELECT * FROM withings_metrics ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWithingsMetrics(): Flow<WithingsMetricsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamsungMetrics(metrics: SamsungHealthMetricsEntity)

    @Query("SELECT * FROM samsung_health_metrics ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSamsungMetrics(): Flow<SamsungHealthMetricsEntity?>
}
