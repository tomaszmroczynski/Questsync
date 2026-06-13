package pl.complet.QuestSync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.complet.QuestSync.ui.components.CyberContainer
import pl.complet.QuestSync.ui.components.CyberHeader
import pl.complet.QuestSync.ui.theme.CyberBlack
import pl.complet.QuestSync.ui.theme.RipperGold
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = RipperGold
                    ),
                    title = { Text("BIO-NEURAL ANALYSIS", letterSpacing = 2.sp, fontWeight = FontWeight.Bold) }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = RipperGold
                )
                
                CyberHeader("Neural Link Status")

                when (val state = uiState) {
                    is AIInsightsUiState.Loading -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            color = RipperGold,
                            trackColor = RipperGold.copy(alpha = 0.1f)
                        )
                        Text(
                            "DECODING BIOMETRIC TELEMETRY...",
                            style = MaterialTheme.typography.labelMedium,
                            color = RipperGold,
                            letterSpacing = 2.sp
                        )
                    }
                    is AIInsightsUiState.Success -> {
                        CyberContainer {
                            Text(
                                text = "CORE INSIGHTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = RipperGold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = state.insights,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                lineHeight = 24.sp
                            )
                        }
                        Button(
                            onClick = { viewModel.fetchInsights() },
                            modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RipperGold, contentColor = CyberBlack)
                        ) {
                            Text("RE-SCAN BIOMETRICS", fontWeight = FontWeight.Bold)
                        }
                    }
                    is AIInsightsUiState.Error -> {
                        CyberContainer {
                            Text(
                                text = "COMMUNICATION FAILURE",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(state.message, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Button(
                            onClick = { viewModel.fetchInsights() },
                            modifier = Modifier.padding(top = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("RETRY NEURAL LINK")
                        }
                    }
                    is AIInsightsUiState.Idle -> {}
                }
            }
        }
    }
}
