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
import pro.sihao.jarvis.ui.screens.ChatScreen
import pro.sihao.jarvis.ui.screens.SettingsScreen
import pro.sihao.jarvis.ui.screens.ProviderListScreen
import pro.sihao.jarvis.ui.screens.ProviderConfigScreen
import pro.sihao.jarvis.ui.screens.ModelSelectorScreen
import pro.sihao.jarvis.ui.screens.ModelConfigScreen
import pro.sihao.jarvis.ui.screens.GlassesScreen
import pro.sihao.jarvis.ui.theme.JarvisTheme

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
                    startDestination = "chat",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("chat") {
                        ChatScreen(
                            onNavigateToSettings = {
                                navController.navigate("settings")
                                }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onNavigateToProviderManagement = {
                                navController.navigate("providers")
                            },
                            onNavigateToGlasses = {
                                navController.navigate("glasses")
                            }
                        )
                    }
                    composable("glasses") {
                        GlassesScreen(
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("providers") {
                        ProviderListScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onAddProviderClick = {
                                navController.navigate("providers/add")
                            },
                            onEditProviderClick = { provider ->
                                navController.navigate("providers/edit/${provider.id}")
                            },
                            onManageModelsClick = { providerId, providerName ->
                                navController.navigate("models/$providerId/$providerName")
                            }
                        )
                    }
                    composable("providers/add") {
                        ProviderConfigScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            providerId = null
                        )
                    }
                    composable("providers/edit/{providerId}") { backStackEntry ->
                        val providerId = backStackEntry.arguments?.getString("providerId")?.toLongOrNull()
                        ProviderConfigScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            providerId = providerId
                        )
                    }
                    composable("models/{providerId}/{providerName}") { backStackEntry ->
                        val providerId = backStackEntry.arguments?.getString("providerId")?.toLongOrNull()
                        val providerName = backStackEntry.arguments?.getString("providerName") ?: "Unknown"
                        ModelSelectorScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            providerId = providerId ?: return@composable,
                            providerName = providerName,
                            onAddModelClick = {
                                providerId?.let { id ->
                                    navController.navigate("model_config/$id/${providerName}/null")
                                }
                            },
                            onEditModelClick = { modelId ->
                                providerId?.let { id ->
                                    navController.navigate("model_config/$id/${providerName}/$modelId")
                                }
                            }
                        )
                    }
                    composable("model_config/{providerId}/{providerName}/{modelId}") { backStackEntry ->
                        val providerId = backStackEntry.arguments?.getString("providerId")?.toLongOrNull()
                        val providerName = backStackEntry.arguments?.getString("providerName") ?: "Unknown"
                        val modelId = backStackEntry.arguments?.getString("modelId")?.toLongOrNull()
                        ModelConfigScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            providerId = providerId ?: return@composable,
                            providerName = providerName,
                            modelId = modelId
                        )
                    }
                }
            }
        }
    }
}
