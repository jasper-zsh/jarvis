package pro.sihao.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import pro.sihao.jarvis.core.presentation.navigation.BottomTabNavigation
import pro.sihao.jarvis.core.presentation.navigation.NavigationManager
import pro.sihao.jarvis.core.presentation.theme.JarvisTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigationManager: NavigationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JarvisTheme {
                BottomTabNavigation(
                    navigationManager = navigationManager,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}