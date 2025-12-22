package pro.sihao.jarvis.core.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pro.sihao.jarvis.features.glasses.presentation.screens.GlassesScreen
import pro.sihao.jarvis.features.realtime.presentation.screens.RealTimeCallScreen
import pro.sihao.jarvis.features.settings.presentation.screens.SettingsScreen
import pro.sihao.jarvis.core.presentation.navigation.TabNavigation

/**
 * Bottom tab navigation component that provides tab switching between primary modules.
 * Integrates with NavigationManager for centralized state management and supports deep linking.
 */
@Composable
fun BottomTabNavigation(
    navigationManager: NavigationManager,
    modifier: Modifier = Modifier
) {
    val navigationState by navigationManager.navigationState.collectAsState()
    val selectedTab = navigationState.selectedTab
    val navController = rememberNavController()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBar {
                        TabNavigation.getAllTabs().forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = {
                                    navigationManager.navigateToTab(tab)
                                    // Update NavController for consistency
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(text = tab.title)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = TabNavigation.getDefault().route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                composable(TabNavigation.Chat.route) {
                    RealTimeCallScreen()
                }
                composable(TabNavigation.Glasses.route) {
                    GlassesScreen()
                }
                composable(TabNavigation.Settings.route) {
                    SettingsScreen(
                        onBackClick = { /* No back action needed for tab */ },
                        onNavigateToGlasses = {
                            navigationManager.navigateToTab(TabNavigation.Glasses)
                        },
                        onNavigateToRealtime = {
                            navigationManager.navigateToTab(TabNavigation.Chat)
                        }
                    )
                }
            }
        }
    }
}