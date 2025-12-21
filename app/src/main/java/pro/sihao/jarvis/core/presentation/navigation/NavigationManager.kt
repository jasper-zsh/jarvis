package pro.sihao.jarvis.core.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized navigation manager that handles all navigation state and programmatic navigation.
 * Eliminates the need for callback passing between components and provides a single source of truth.
 */
@Singleton
class NavigationManager @Inject constructor() {

    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    /**
     * Navigate to a specific tab
     */
    fun navigateToTab(tab: TabNavigation) {
        _navigationState.value = _navigationState.value.copy(
            selectedTab = tab,
            navigationHistory = _navigationState.value.navigationHistory + tab
        )
    }

    /**
     * Navigate to a specific tab with deep link
     */
    fun navigateToDeepLink(deepLink: String) {
        val targetTab = TabNavigation.fromDeepLink(deepLink)
        if (targetTab != null) {
            navigateToTab(targetTab)
        }
    }

    /**
     * Get the current selected tab
     */
    fun getCurrentTab(): TabNavigation = _navigationState.value.selectedTab

    /**
     * Check if a specific tab is currently selected
     */
    fun isTabSelected(tab: TabNavigation): Boolean {
        return _navigationState.value.selectedTab == tab
    }

    /**
     * Reset navigation to default state
     */
    fun resetToDefault() {
        _navigationState.value = NavigationState()
    }
}