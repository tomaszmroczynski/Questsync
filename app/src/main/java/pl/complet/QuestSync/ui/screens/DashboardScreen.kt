package pl.complet.QuestSync.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.complet.QuestSync.data.local.QuestActivityEntity
import pl.complet.QuestSync.ui.viewmodels.HealthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HealthViewModel,
    onNavigateToQuest: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToAI: () -> Unit
) {
    val questActivities by viewModel.questActivities.collectAsStateWithLifecycle(initialValue = emptyList())
    val latestOura by viewModel.latestOuraMetrics.collectAsStateWithLifecycle(initialValue = null)
    val latestWithings by viewModel.latestWithingsMetrics.collectAsStateWithLifecycle(initialValue = null)
    val latestSamsung by viewModel.latestSamsungMetrics.collectAsStateWithLifecycle(initialValue = null)
    val snifferActive by viewModel.snifferActive.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("QuestSync Dashboard", fontWeight = FontWeight.Bold)
                        if (snifferActive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "LIVE VR Sync Active", 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSniffer(context) }) {
                        Icon(
                            if (snifferActive) Icons.Rounded.StopCircle else Icons.Rounded.PlayCircle,
                            contentDescription = "Toggle Sniffer",
                            tint = if (snifferActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.syncAll() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Sync All")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Insights Hero Section
            item {
                Card(
                    onClick = onNavigateToAI,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "AI Health Insights",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "See how VR workouts affect your recovery.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    "Unified Metrics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Quest Activity Card
            item {
                MetricCard(
                    title = "Quest Activity",
                    value = "${questActivities.sumOf { it.durationMinutes }} min",
                    subtitle = "${questActivities.size} workouts synced",
                    icon = Icons.Rounded.Vrpano,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onNavigateToQuest
                )
            }

            // Health Sources Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = "Oura Sleep",
                        value = "${latestOura?.sleepDurationHours?.let { "%.1f".format(it) } ?: "--"}h",
                        subtitle = "Readiness: ${latestOura?.readinessScore ?: "--"}",
                        icon = Icons.Rounded.Bedtime,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHealth
                    )
                    MetricCard(
                        title = "Samsung",
                        value = "${latestSamsung?.stepCount ?: "--"}",
                        subtitle = "Steps today",
                        icon = Icons.Rounded.DirectionsWalk,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHealth
                    )
                }
            }

            // Withings Card
            item {
                MetricCard(
                    title = "Withings Health",
                    value = "${latestWithings?.weightKg ?: "--"} kg",
                    subtitle = "BP: ${latestWithings?.bloodPressureSystolic ?: "--"}/${latestWithings?.bloodPressureDiastolic ?: "--"}",
                    icon = Icons.Rounded.MonitorWeight,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = onNavigateToHealth
                )
            }

            item {
                Text(
                    "Recent Workouts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(questActivities.take(3)) { activity ->
                WorkoutItem(activity)
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun WorkoutItem(activity: QuestActivityEntity) {
    ListItem(
        headlineContent = { Text(activity.activityName) },
        supportingContent = { Text("${activity.durationMinutes} min • ${activity.caloriesBurned} kcal") },
        leadingContent = {
            Icon(
                Icons.Rounded.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Text(
                java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(activity.timestamp)),
                style = MaterialTheme.typography.labelSmall
            )
        }
    )
}
