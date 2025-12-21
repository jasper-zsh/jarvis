package pro.sihao.jarvis.core.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class hierarchy for tab definitions providing type-safe tab configurations.
 * Easy to extend with new tabs without modifying navigation logic.
 */
sealed class TabNavigation(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val deepLinkPrefix: String
) {
    /**
     * Chat tab for realtime voice chat interface
     */
    data object Chat : TabNavigation(
        route = "chat",
        title = "Chat",
        icon = Icons.Default.Chat,
        deepLinkPrefix = "jarvis://chat"
    )

    /**
     * Glasses tab for glasses connection and management
     */
    data object Glasses : TabNavigation(
        route = "glasses",
        title = "Glasses",
        icon = Icons.Default.ViewInAr,
        deepLinkPrefix = "jarvis://glasses"
    )

    companion object {
        /**
         * Get all available tabs
         */
        fun getAllTabs(): List<TabNavigation> = listOf(Chat, Glasses)

        /**
         * Find tab by route
         */
        fun fromRoute(route: String): TabNavigation? = when (route) {
            Chat.route -> Chat
            Glasses.route -> Glasses
            else -> null
        }

        /**
         * Find tab from deep link
         */
        fun fromDeepLink(deepLink: String): TabNavigation? = getAllTabs().find {
            deepLink.startsWith(it.deepLinkPrefix)
        }

        /**
         * Get default tab
         */
        fun getDefault(): TabNavigation = Chat
    }
}