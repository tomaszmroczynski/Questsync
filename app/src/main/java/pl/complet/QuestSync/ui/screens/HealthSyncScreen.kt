package pl.complet.QuestSync.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthSyncScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Health Integrations") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Wearable Sync Status",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            SourceStatusItem("Oura Ring", true)
            SourceStatusItem("Withings Scale", true)
            SourceStatusItem("Samsung Health", true)
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Sources")
            }
        }
    }
}

@Composable
fun SourceStatusItem(name: String, isConnected: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) MaterialTheme.colorScheme.secondaryContainer 
                             else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(if (isConnected) "Connected" else "Disconnected")
        }
    }
}
