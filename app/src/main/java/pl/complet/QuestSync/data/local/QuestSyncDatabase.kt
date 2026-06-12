package pl.complet.QuestSync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        QuestActivityEntity::class,
        OuraMetricsEntity::class,
        WithingsMetricsEntity::class,
        SamsungHealthMetricsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class QuestSyncDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao
}
