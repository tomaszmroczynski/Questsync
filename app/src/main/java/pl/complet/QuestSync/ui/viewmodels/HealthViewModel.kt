package pl.complet.QuestSync.ui.viewmodels

import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pl.complet.QuestSync.BuildConfig
import pl.complet.QuestSync.data.repository.HealthRepository

import androidx.lifecycle.ViewModelProvider
import android.content.Context
import pl.complet.QuestSync.data.DataModule

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HealthViewModel(private val repository: HealthRepository) : ViewModel() {
    val questActivities = repository.questActivities
    val latestOuraMetrics = repository.latestOuraMetrics
    val latestWithingsMetrics = repository.latestWithingsMetrics
    val latestSamsungMetrics = repository.latestSamsungMetrics

    private val _questConnectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val questConnectionStatus: StateFlow<ConnectionStatus> = _questConnectionStatus

    private val _snifferActive = MutableStateFlow(false)
    val snifferActive: StateFlow<Boolean> = _snifferActive

    fun toggleSniffer(context: Context) {
        val intent = Intent(context, pl.complet.QuestSync.service.QuestSnifferService::class.java)
        if (_snifferActive.value) {
            context.stopService(intent)
            _snifferActive.value = false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _snifferActive.value = true
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            syncQuest()
            if (BuildConfig.OURA_API_KEY.isNotEmpty()) {
                try {
                    repository.syncOuraData(BuildConfig.OURA_API_KEY)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (BuildConfig.WITHINGS_API_KEY.isNotEmpty()) {
                try {
                    repository.syncWithingsData(BuildConfig.WITHINGS_API_KEY)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun syncQuest() {
        viewModelScope.launch {
            if (BuildConfig.META_QUEST_API_KEY.isEmpty()) {
                _questConnectionStatus.value = ConnectionStatus.Error("API Key missing in local.properties")
                return@launch
            }
            
            _questConnectionStatus.value = ConnectionStatus.Loading
            try {
                repository.syncQuestData(BuildConfig.META_QUEST_API_KEY)
                _questConnectionStatus.value = ConnectionStatus.Success
            } catch (e: Exception) {
                e.printStackTrace()
                _questConnectionStatus.value = ConnectionStatus.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class ConnectionStatus {
    data object Idle : ConnectionStatus()
    data object Loading : ConnectionStatus()
    data object Success : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

class HealthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthViewModel::class.java)) {
            val repository = DataModule.provideHealthRepository(context)
            @Suppress("UNCHECKED_CAST")
            return HealthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
