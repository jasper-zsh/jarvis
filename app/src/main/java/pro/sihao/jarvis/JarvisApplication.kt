package pro.sihao.jarvis

import android.app.Application
import android.content.Intent
import dagger.hilt.android.HiltAndroidApp
import pro.sihao.jarvis.core.data.database.initializer.DatabaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pro.sihao.jarvis.service.GlassesConnectionService
import javax.inject.Inject

@HiltAndroidApp
class JarvisApplication : Application() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    override fun onCreate() {
        super.onCreate()

        // Initialize database in background
        CoroutineScope(Dispatchers.IO).launch {
            databaseInitializer.initializeIfNeeded()
        }

        // Start background service to keep Rokid glasses auto-reconnect active.
        startService(Intent(this, GlassesConnectionService::class.java))
    }
}
