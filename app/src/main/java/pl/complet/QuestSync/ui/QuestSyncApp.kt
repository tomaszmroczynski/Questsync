package pl.complet.QuestSync.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import pl.complet.QuestSync.navigation.Destination
import pl.complet.QuestSync.ui.screens.AIInsightsScreen
import pl.complet.QuestSync.ui.screens.DashboardScreen
import pl.complet.QuestSync.ui.screens.HealthSyncScreen
import pl.complet.QuestSync.ui.screens.QuestSyncScreen
import pl.complet.QuestSync.ui.theme.QuestSyncTheme
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.complet.QuestSync.ui.components.HealthPermissionHandler
import pl.complet.QuestSync.ui.viewmodels.AIInsightsViewModel
import pl.complet.QuestSync.ui.viewmodels.AIInsightsViewModelFactory
import pl.complet.QuestSync.ui.viewmodels.HealthViewModel
import pl.complet.QuestSync.ui.viewmodels.HealthViewModelFactory

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun QuestSyncApp() {
    val context = LocalContext.current
    val healthViewModel: HealthViewModel = viewModel(factory = HealthViewModelFactory(context))
    val aiViewModel: AIInsightsViewModel = viewModel(factory = AIInsightsViewModelFactory(context))

    QuestSyncTheme {
        HealthPermissionHandler {
            val backStack = rememberNavBackStack(Destination.Dashboard)
            val adaptiveInfo = currentWindowAdaptiveInfo()
            val directive = remember(adaptiveInfo) {
                calculatePaneScaffoldDirective(adaptiveInfo)
                    .copy(horizontalPartitionSpacerSize = 0.dp)
            }
            val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.size - 1) },
                sceneStrategy = listDetailStrategy,
                entryProvider = entryProvider {
                    entry<Destination.Dashboard>(
                        metadata = ListDetailSceneStrategy.listPane()
                    ) {
                        DashboardScreen(
                            viewModel = healthViewModel,
                            onNavigateToQuest = { backStack.add(Destination.QuestSync) },
                            onNavigateToHealth = { backStack.add(Destination.HealthSync) },
                            onNavigateToAI = { backStack.add(Destination.AIInsights) }
                        )
                    }
                    entry<Destination.QuestSync>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) {
                        QuestSyncScreen(healthViewModel)
                    }
                    entry<Destination.HealthSync>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) {
                        HealthSyncScreen()
                    }
                    entry<Destination.AIInsights>(
                        metadata = ListDetailSceneStrategy.extraPane()
                    ) {
                        AIInsightsScreen(aiViewModel)
                    }
                }
            )
        }
    }
}
