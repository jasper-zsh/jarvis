package pro.sihao.jarvis.data.database.initializer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database initializer - simplified for PipeCat-only architecture
 * No LLM provider initialization needed since we use only PipeCat
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val initializedKey = "database_initialized_v3"

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        val isInitialized = prefs.getBoolean(initializedKey, false)

        if (!isInitialized) {
            // Initialize any default data needed for PipeCat-only architecture
            // Currently no default initialization needed
            prefs.edit()
                .putBoolean(initializedKey, true)
                .apply()
        }
    }
}