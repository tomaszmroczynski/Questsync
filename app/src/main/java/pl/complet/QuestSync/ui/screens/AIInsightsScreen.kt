package pl.complet.QuestSync.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.complet.QuestSync.ui.viewmodels.AIInsightsUiState
import pl.complet.QuestSync.ui.viewmodels.AIInsightsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIInsightsScreen(viewModel: AIInsightsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (uiState is AIInsightsUiState.Idle) {
            viewModel.fetchInsights()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Claude AI Insights") },
                navigationIcon = {
                    // This might be shown depending on the adaptive layout
                    // but usually the scaffold handles back
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is AIInsightsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                    Text(
                        "Analyzing your health and VR data...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                is AIInsightsUiState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Personalized Summary",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = state.insights,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified // Or some good line height
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.fetchInsights() },
                        modifier = Modifier.padding(top = 24.dp).fillMaxWidth()
                    ) {
                        Text("Regenerate Insights")
                    }
                }
                is AIInsightsUiState.Error -> {
                    Text(
                        text = "Something went wrong",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(state.message, style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { viewModel.fetchInsights() },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Retry Connection")
                    }
                }
                is AIInsightsUiState.Idle -> {}
            }
        }
    }
}
