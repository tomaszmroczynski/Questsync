package pl.complet.QuestSync.data

import android.content.Context
import androidx.room.Room
import pl.complet.QuestSync.data.local.QuestSyncDatabase
import pl.complet.QuestSync.data.remote.OuraApi
import pl.complet.QuestSync.data.remote.QuestApi
import pl.complet.QuestSync.data.remote.WithingsApi
import pl.complet.QuestSync.data.repository.HealthRepository
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object DataModule {
    private var database: QuestSyncDatabase? = null

    fun provideDatabase(context: Context): QuestSyncDatabase {
        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                QuestSyncDatabase::class.java,
                "quest_sync_db"
            ).build()
            database = instance
            instance
        }
    }

    fun provideQuestApi(): QuestApi {
        return Retrofit.Builder()
            .baseUrl("https://api.oculus.com/v1/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(QuestApi::class.java)
    }

    fun provideOuraApi(): OuraApi {
        return Retrofit.Builder()
            .baseUrl("https://api.ouraring.com/v2/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(OuraApi::class.java)
    }

    fun provideWithingsApi(): WithingsApi {
        return Retrofit.Builder()
            .baseUrl("https://wbsapi.withings.net/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(WithingsApi::class.java)
    }

    fun provideHealthRepository(context: Context): HealthRepository {
        return HealthRepository(
            healthDao = provideDatabase(context).healthDao(),
            ouraApi = provideOuraApi(),
            withingsApi = provideWithingsApi(),
            questApi = provideQuestApi()
        )
    }

    fun provideAIAggregator(context: Context): AIAggregator {
        return AIAggregator(provideHealthRepository(context))
    }

    private var mcpClientManager: McpClientManager? = null

    fun provideMcpClientManager(): McpClientManager {
        return mcpClientManager ?: synchronized(this) {
            val instance = McpClientManager(pl.complet.QuestSync.BuildConfig.MCP_SERVER_URL)
            mcpClientManager = instance
            instance
        }
    }
}
