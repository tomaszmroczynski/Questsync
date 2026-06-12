package pl.complet.QuestSync.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data object Dashboard : Destination

    @Serializable
    data object QuestSync : Destination

    @Serializable
    data object HealthSync : Destination

    @Serializable
    data object AIInsights : Destination
}
