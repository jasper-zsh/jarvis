package pro.sihao.jarvis.data.database.migration

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pro.sihao.jarvis.data.database.JarvisDatabase
import pro.sihao.jarvis.data.database.entity.LLMProviderEntity
import pro.sihao.jarvis.data.database.entity.ModelConfigEntity
import pro.sihao.jarvis.data.storage.SecureStorage
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsMigrationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: JarvisDatabase,
    private val secureStorage: SecureStorage
) {
    private val migrationCompleted = AtomicBoolean(false)

    suspend fun migrateIfNeeded() = withContext(Dispatchers.IO) {
        if (migrationCompleted.get()) return@withContext

        try {
            val hasMigrated = context.getSharedPreferences("jarvis_migration_prefs", Context.MODE_PRIVATE)
                .getBoolean("settings_migrated_to_db", false)

            if (!hasMigrated) {
                performMigration()

                // Mark migration as completed
                context.getSharedPreferences("jarvis_migration_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("settings_migrated_to_db", true)
                    .apply()
            }
        } catch (e: Exception) {
            // Log migration error but don't crash the app
            e.printStackTrace()
        } finally {
            migrationCompleted.set(true)
        }
    }

    private suspend fun performMigration() = withContext(Dispatchers.IO) {
        val providerDao = database.llmProviderDao()
        val modelConfigDao = database.modelConfigDao()

        // Get existing settings from SecureStorage
        val currentProvider = secureStorage.getApiProvider()
        val baseUrl = secureStorage.getBaseUrl()
        val modelName = secureStorage.getModelName()
        val temperature = secureStorage.getTemperature()
        val maxTokens = secureStorage.getMaxTokens()

        // Create default providers based on current settings and known providers
        val defaultProviders = createDefaultProviders()

        // Insert providers and get their IDs
        val providerIds = defaultProviders.map { provider ->
            val existingProvider = providerDao.getProviderByName(provider.name)
            existingProvider?.id ?: providerDao.insertProvider(provider)
        }

        // Create model config for the user's current selection if available
        if (modelName.isNotEmpty()) {
            val openAIProviderId = providerIds.find { pid ->
                val provider = providerDao.getProviderById(pid)
                provider?.name == "OPENAI"
            }

            openAIProviderId?.let { pid ->
                val existingModel = modelConfigDao.getModelConfigByProviderAndModel(pid, modelName)
                if (existingModel == null) {
                    val modelConfig = ModelConfigEntity(
                        providerId = pid,
                        modelName = modelName,
                        displayName = modelName,
                        maxTokens = if (maxTokens > 0) maxTokens else null,
                        contextWindow = null,
                        inputCostPer1K = null,
                        outputCostPer1K = null,
                        temperature = temperature,
                        isActive = true,
                        isDefault = true
                    )
                    modelConfigDao.insertModelConfig(modelConfig)
                }
            }
        }
    }

    private fun createDefaultProviders(): List<LLMProviderEntity> {
        val currentTime = System.currentTimeMillis()

        return listOf(
            LLMProviderEntity(
                name = "OPENAI",
                displayName = "OpenAI",
                baseUrl = "https://api.openai.com/v1/",
                authenticationType = "API_KEY",
                defaultModel = "gpt-3.5-turbo",
                isActive = true,
                supportsModelDiscovery = true,
                maxTokens = 4096,
                description = "OpenAI's GPT models",
                createdTimestamp = currentTime,
                updatedTimestamp = currentTime
            ),
            LLMProviderEntity(
                name = "DEEPSEEK",
                displayName = "DeepSeek",
                baseUrl = "https://api.deepseek.com/v1/",
                authenticationType = "API_KEY",
                defaultModel = "deepseek-chat",
                isActive = true,
                supportsModelDiscovery = true,
                maxTokens = 4096,
                description = "DeepSeek AI models",
                createdTimestamp = currentTime,
                updatedTimestamp = currentTime
            ),
            LLMProviderEntity(
                name = "LOCAL_AI",
                displayName = "Local AI",
                baseUrl = "http://localhost:11434/v1/",
                authenticationType = "API_KEY",
                defaultModel = "llama2",
                isActive = false,
                supportsModelDiscovery = false,
                description = "Local AI server (Ollama, LM Studio)",
                createdTimestamp = currentTime,
                updatedTimestamp = currentTime
            ),
            LLMProviderEntity(
                name = "TOGETHER_AI",
                displayName = "Together AI",
                baseUrl = "https://api.together.xyz/v1/",
                authenticationType = "API_KEY",
                defaultModel = "meta-llama/Llama-2-7b-chat-hf",
                isActive = false,
                supportsModelDiscovery = true,
                description = "Together AI open models",
                createdTimestamp = currentTime,
                updatedTimestamp = currentTime
            ),
            LLMProviderEntity(
                name = "GROQ",
                displayName = "Groq",
                baseUrl = "https://api.groq.com/openai/v1/",
                authenticationType = "API_KEY",
                defaultModel = "llama3-8b-8192",
                isActive = false,
                supportsModelDiscovery = true,
                description = "Groq fast inference",
                createdTimestamp = currentTime,
                updatedTimestamp = currentTime
            )
        )
    }

    suspend fun resetMigration() = withContext(Dispatchers.IO) {
        try {
            // Clear all providers and model configs
            database.llmProviderDao().let { dao ->
                dao.getAllProviders().collect { providers ->
                    providers.forEach { dao.deleteProvider(it) }
                }
            }

            // Reset migration flag
            context.getSharedPreferences("jarvis_migration_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("settings_migrated_to_db")
                .apply()

            migrationCompleted.set(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}