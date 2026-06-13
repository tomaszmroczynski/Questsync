package pl.complet.QuestSync.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.complet.QuestSync.data.local.QuestActivityEntity
import pl.complet.QuestSync.ui.components.CyberContainer
import pl.complet.QuestSync.ui.components.CyberHeader
import pl.complet.QuestSync.ui.viewmodels.HealthViewModel
import pl.complet.QuestSync.ui.theme.CyberBlack
import pl.complet.QuestSync.ui.theme.RipperGold

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
    
    val hasUsageAccess = remember { mutableStateOf(checkUsageStatsPermission(context)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RipperGold.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = 1000f
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = RipperGold
                    ),
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "QUESTSYNC // RI-PPER",
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp,
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (snifferActive) {
                                Text(
                                    "LIVE TELEMETRY ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            viewModel.toggleSniffer(context)
                            hasUsageAccess.value = checkUsageStatsPermission(context)
                        }) {
                            Icon(
                                if (snifferActive) Icons.Rounded.StopCircle else Icons.Rounded.PlayCircle,
                                contentDescription = "Toggle Sniffer",
                                tint = if (snifferActive) MaterialTheme.colorScheme.error else RipperGold
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (snifferActive && !hasUsageAccess.value) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "USAGE ACCESS MISSING: Cannot detect VR apps. Enable in Quest Settings.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                item {
                    CyberContainer(onClick = onNavigateToAI) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "NEURAL INSIGHTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RipperGold,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    "BIO-ANALYSIS",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Icon(
                                Icons.Rounded.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = RipperGold
                            )
                        }
                    }
                }

                item { CyberHeader("Telemetry Modules") }

                item {
                    CyberMetricCard(
                        title = "VR ACTIVITY",
                        value = "${questActivities.sumOf { it.durationMinutes }} MIN",
                        subtitle = "SESSIONS: ${questActivities.size}",
                        icon = Icons.Rounded.Hub,
                        onClick = onNavigateToQuest
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CyberMetricCard(
                            title = "OURA",
                            value = "${latestOura?.readinessScore ?: "--"}",
                            subtitle = "READINESS",
                            icon = Icons.Rounded.Grain,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToHealth
                        )
                        CyberMetricCard(
                            title = "SAMSUNG",
                            value = "${latestSamsung?.stepCount ?: "--"}",
                            subtitle = "STEPS",
                            icon = Icons.Rounded.Bolt,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToHealth
                        )
                    }
                }

                item {
                    CyberMetricCard(
                        title = "BIOMETRICS",
                        value = "${latestWithings?.weightKg ?: "--"} KG",
                        subtitle = "WITHINGS CORE",
                        icon = Icons.Rounded.SettingsInputComponent,
                        onClick = onNavigateToHealth
                    )
                }

                item { CyberHeader("Recent Data Stream") }

                items(questActivities.take(5)) { activity ->
                    CyberWorkoutItem(activity)
                }
            }
        }
    }
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

@Composable
fun CyberMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    CyberContainer(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = RipperGold,
                    letterSpacing = 1.sp
                )
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = RipperGold
            )
        }
    }
}

@Composable
fun CyberWorkoutItem(activity: QuestActivityEntity) {
    CyberContainer(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Terminal,
                contentDescription = null,
                tint = RipperGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activity.activityName.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "${activity.durationMinutes}M // ${activity.caloriesBurned}KCAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = RipperGold
                )
            }
            Text(
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(activity.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray
            )
        }
    }
}
