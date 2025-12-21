package pro.sihao.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pro.sihao.jarvis.features.settings.presentation.screens.SettingsScreen
import pro.sihao.jarvis.features.glasses.presentation.screens.GlassesScreen
import pro.sihao.jarvis.features.realtime.presentation.screens.RealTimeCallScreen
import pro.sihao.jarvis.core.presentation.theme.JarvisTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JarvisTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "realtime",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("realtime") {
                        RealTimeCallScreen(
                            onNavigateToTextMode = {
                                // Text mode removed - navigate to settings instead
                                navController.navigate("settings")
                            },
                            onNavigateToGlassesMode = {
                                navController.navigate("glasses")
                            }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onNavigateToGlasses = {
                                navController.navigate("glasses")
                            },
                            onNavigateToRealtime = {
                                navController.navigate("realtime")
                            }
                        )
                    }
                    composable("glasses") {
                        GlassesScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onNavigateToRealtime = {
                                navController.navigate("realtime")
                            }
                        )
                    }
                }
            }
        }
    }
}
