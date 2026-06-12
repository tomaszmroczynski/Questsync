package pl.complet.QuestSync.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pl.complet.QuestSync.data.AIAggregator
import pl.complet.QuestSync.data.DataModule
import pl.complet.QuestSync.data.McpClientManager
import kotlinx.coroutines.CancellationException

class AIInsightsViewModel(
    private val aggregator: AIAggregator,
    private val mcpManager: McpClientManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIInsightsUiState>(AIInsightsUiState.Idle)
    val uiState: StateFlow<AIInsightsUiState> = _uiState

    fun fetchInsights() {
        viewModelScope.launch {
            _uiState.value = AIInsightsUiState.Loading
            try {
                val data = aggregator.getAggregatedDataString()
                val insights = mcpManager.getInsights(data)
                
                // Check if the result is an error message returned as a string from McpClientManager
                if (insights.startsWith("Error") || insights.contains("timed out")) {
                    _uiState.value = AIInsightsUiState.Error(insights)
                } else {
                    _uiState.value = AIInsightsUiState.Success(insights)
                }
            } catch (e: CancellationException) {
                // Do nothing, normal cancellation
                throw e
            } catch (e: Exception) {
                _uiState.value = AIInsightsUiState.Error(e.message ?: "Unknown error occurred while fetching insights.")
            }
        }
    }
}

sealed class AIInsightsUiState {
    data object Idle : AIInsightsUiState()
    data object Loading : AIInsightsUiState()
    data class Success(val insights: String) : AIInsightsUiState()
    data class Error(val message: String) : AIInsightsUiState()
}

class AIInsightsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AIInsightsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AIInsightsViewModel(
                DataModule.provideAIAggregator(context),
                DataModule.provideMcpClientManager()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
