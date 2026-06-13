package pl.complet.QuestSync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Vrpano
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.complet.QuestSync.ui.components.CyberContainer
import pl.complet.QuestSync.ui.theme.CyberBlack
import pl.complet.QuestSync.ui.theme.RipperGold
import pl.complet.QuestSync.ui.viewmodels.ConnectionStatus
import pl.complet.QuestSync.ui.viewmodels.HealthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestSyncScreen(viewModel: HealthViewModel) {
    val status by viewModel.questConnectionStatus.collectAsStateWithLifecycle()
    val activities by viewModel.questActivities.collectAsStateWithLifecycle(initialValue = emptyList())

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
                    title = { Text("VR TELEMETRY LINK", letterSpacing = 2.sp) }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
            ) {
                Icon(
                    Icons.Rounded.Hub,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = RipperGold
                )
                
                Text(
                    "QUEST-NODE: ONLINE",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                CyberConnectionStatusIndicator(status)

                if (activities.isNotEmpty()) {
                    CyberContainer {
                        Text(
                            "BUFFERED SESSIONS: ${activities.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = RipperGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = { viewModel.syncQuest() },
                    enabled = status !is ConnectionStatus.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RipperGold, contentColor = CyberBlack)
                ) {
                    if (status is ConnectionStatus.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = CyberBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("FORCE DATA SYNC", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RipperGold),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RipperGold)
                ) {
                    Text("RE-AUTH NEURAL INTERFACE")
                }
            }
        }
    }
}

@Composable
fun CyberConnectionStatusIndicator(status: ConnectionStatus) {
    val color = when (status) {
        is ConnectionStatus.Idle -> Color.Gray
        is ConnectionStatus.Loading -> RipperGold
        is ConnectionStatus.Success -> Color.Cyan
        is ConnectionStatus.Error -> MaterialTheme.colorScheme.error
    }
    
    val text = when (status) {
        is ConnectionStatus.Idle -> "LINK READY"
        is ConnectionStatus.Loading -> "TRANSMITTING..."
        is ConnectionStatus.Success -> "SYNC COMPLETE"
        is ConnectionStatus.Error -> "LINK FAILURE: ${status.message.uppercase()}"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            letterSpacing = 1.sp
        )
    }
}
