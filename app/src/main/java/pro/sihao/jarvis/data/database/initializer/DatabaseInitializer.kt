package pro.sihao.jarvis.data.database.initializer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pro.sihao.jarvis.data.database.entity.LLMProviderEntity
import pro.sihao.jarvis.data.database.dao.LLMProviderDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmProviderDao: LLMProviderDao
) {
    private val initializedKey = "database_initialized_v2"

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        val isInitialized = prefs.getBoolean(initializedKey, false)

        if (!isInitialized) {
            initializeDefaultProviders()
            prefs.edit()
                .putBoolean(initializedKey, true)
                .apply()
        }
    }

    private suspend fun initializeDefaultProviders() {
        val currentTime = System.currentTimeMillis()

        val defaultProviders = listOf(
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
                isActive = false,
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

        defaultProviders.forEach { provider ->
            try {
                llmProviderDao.insertProvider(provider)
            } catch (e: Exception) {
                // Ignore duplicate provider errors
            }
        }
    }
}