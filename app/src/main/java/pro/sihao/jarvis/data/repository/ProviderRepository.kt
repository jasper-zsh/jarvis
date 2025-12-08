package pro.sihao.jarvis.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pro.sihao.jarvis.data.database.dao.LLMProviderDao
import pro.sihao.jarvis.data.database.entity.LLMProviderEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val llmProviderDao: LLMProviderDao
) {

    fun getAllProviders(): Flow<List<LLMProviderEntity>> = llmProviderDao.getAllProviders()

    fun getActiveProviders(): Flow<List<LLMProviderEntity>> = llmProviderDao.getActiveProviders()

    suspend fun getProviderById(id: Long): LLMProviderEntity? = llmProviderDao.getProviderById(id)

    suspend fun getProviderByName(name: String): LLMProviderEntity? = llmProviderDao.getProviderByName(name)

    suspend fun insertProvider(provider: LLMProviderEntity): Long {
        return llmProviderDao.insertProvider(provider.copy(
            updatedTimestamp = System.currentTimeMillis()
        ))
    }

    suspend fun updateProvider(provider: LLMProviderEntity) {
        llmProviderDao.updateProvider(provider.copy(
            updatedTimestamp = System.currentTimeMillis()
        ))
    }

    suspend fun deleteProvider(provider: LLMProviderEntity) {
        llmProviderDao.deleteProvider(provider)
    }

    suspend fun deleteProviderById(id: Long) {
        llmProviderDao.deleteProviderById(id)
    }

    suspend fun setActiveProvider(providerId: Long) {
        // Set all providers to inactive except the selected one
        llmProviderDao.setAllProvidersInactiveExcept(providerId)
        llmProviderDao.updateProviderActiveStatus(providerId, true)
    }

    suspend fun getActiveProvider(): LLMProviderEntity? {
        return try {
            // This is a simplified approach - in a real implementation, you might want to
            // collect the Flow or use a different method to get the first active provider
            null // Placeholder - would need proper Flow collection
        } catch (e: Exception) {
            null
        }
    }

    suspend fun hasActiveProvider(): Boolean {
        return llmProviderDao.getActiveProviderCount() > 0
    }

    suspend fun getProviderCount(): Int = llmProviderDao.getProviderCount()

    // Convenience methods for provider configuration
    fun getProviderConfiguration(providerName: String): Flow<ProviderConfig?> {
        return getActiveProviders().map { providers ->
            providers.find { it.name == providerName }?.let { provider ->
                ProviderConfig(
                    id = provider.id,
                    name = provider.name,
                    displayName = provider.displayName,
                    baseUrl = provider.baseUrl,
                    authenticationType = provider.authenticationType,
                    defaultModel = provider.defaultModel,
                    isActive = provider.isActive,
                    supportsModelDiscovery = provider.supportsModelDiscovery,
                    maxTokens = provider.maxTokens,
                    description = provider.description
                )
            }
        }
    }
}

data class ProviderConfig(
    val id: Long,
    val name: String,
    val displayName: String,
    val baseUrl: String,
    val authenticationType: String,
    val defaultModel: String?,
    val isActive: Boolean,
    val supportsModelDiscovery: Boolean,
    val maxTokens: Int?,
    val description: String?
)