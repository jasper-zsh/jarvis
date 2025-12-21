package pro.sihao.jarvis.core.presentation.navigation

/**
 * Data class for managing navigation state including selected tab and navigation history.
 * Provides immutable state for predictable UI updates.
 */
data class NavigationState(
    val selectedTab: TabNavigation = TabNavigation.getDefault(),
    val navigationHistory: List<TabNavigation> = listOf(TabNavigation.getDefault())
)