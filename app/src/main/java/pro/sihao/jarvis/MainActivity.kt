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
                            }
                        )
                    }
                }
            }
        }
    }
}