package pl.complet.QuestSync.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Vrpano
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.complet.QuestSync.ui.viewmodels.ConnectionStatus
import pl.complet.QuestSync.ui.viewmodels.HealthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestSyncScreen(viewModel: HealthViewModel) {
    val status by viewModel.questConnectionStatus.collectAsStateWithLifecycle()
    val activities by viewModel.questActivities.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Meta Quest Sync") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Icon(
                Icons.Rounded.Vrpano,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                "Meta Quest Connection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            ConnectionStatusIndicator(status)

            if (activities.isNotEmpty()) {
                Text(
                    "Latest sync: ${activities.size} activities found.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.syncQuest() },
                enabled = status !is ConnectionStatus.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (status is ConnectionStatus.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Check Connection & Sync")
                }
            }

            OutlinedButton(
                onClick = { /* In a real app, this might trigger an OAuth flow */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Re-authenticate Oculus Account")
            }
        }
    }
}

@Composable
fun ConnectionStatusIndicator(status: ConnectionStatus) {
    val color = when (status) {
        is ConnectionStatus.Idle -> MaterialTheme.colorScheme.outline
        is ConnectionStatus.Loading -> MaterialTheme.colorScheme.primary
        is ConnectionStatus.Success -> MaterialTheme.colorScheme.primary
        is ConnectionStatus.Error -> MaterialTheme.colorScheme.error
    }
    
    val text = when (status) {
        is ConnectionStatus.Idle -> "Not connected"
        is ConnectionStatus.Loading -> "Connecting..."
        is ConnectionStatus.Success -> "Connected & Synced"
        is ConnectionStatus.Error -> "Error: ${status.message}"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
    }
}
