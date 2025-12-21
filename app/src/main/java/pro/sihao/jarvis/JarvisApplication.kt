package pro.sihao.jarvis

import android.app.Application
import android.content.Intent
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pro.sihao.jarvis.core.initialization.AppInitializationCoordinator
import pro.sihao.jarvis.service.GlassesConnectionService
import javax.inject.Inject

@HiltAndroidApp
class JarvisApplication : Application() {

    @Inject
    lateinit var initializationCoordinator: AppInitializationCoordinator

    override fun onCreate() {
        super.onCreate()
        Log.d("JarvisApplication", "Application onCreate started")

        // Initialize app with proper sequencing
        initializationCoordinator.initialize()

        // Start background service for persistent connection
        // Note: Service now handles proper initialization timing internally
        CoroutineScope(Dispatchers.Main).launch {
            // Wait a bit for initialization to start before starting service
            kotlinx.coroutines.delay(1000)
            try {
                startService(Intent(this@JarvisApplication, GlassesConnectionService::class.java))
                Log.d("JarvisApplication", "GlassesConnectionService started")
            } catch (e: Exception) {
                Log.e("JarvisApplication", "Error starting GlassesConnectionService", e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        initializationCoordinator.cleanup()
        Log.d("JarvisApplication", "Application terminated and cleaned up")
    }
}
