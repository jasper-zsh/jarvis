package pro.sihao.jarvis.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import pro.sihao.jarvis.data.database.dao.LLMProviderDao
import pro.sihao.jarvis.data.database.entity.LLMProviderEntity
import pro.sihao.jarvis.data.encryption.ApikeyEncryption
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val llmProviderDao: LLMProviderDao,
    private val encryption: ApikeyEncryption
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

    suspend fun getProviderCount(): Int = llmProviderDao.getProviderCount()

    suspend fun hasActiveProvider(): Boolean {
        return llmProviderDao.getActiveProviderCount() > 0
    }

    // API Key management methods
    suspend fun saveApiKeyForProvider(providerId: Long, apiKey: String) {
        val provider = getProviderById(providerId) ?: return
        val encryptedKeyRef = encryption.encryptApiKey(apiKey)
        updateProvider(provider.copy(encryptedApiKey = encryptedKeyRef))
    }

    suspend fun getApiKeyForProvider(providerId: Long): String? {
        val provider = getProviderById(providerId)
        return provider?.let { encryption.decryptApiKey(it.encryptedApiKey) }
    }

    suspend fun hasApiKeyForProvider(providerId: Long): Boolean {
        return !getApiKeyForProvider(providerId).isNullOrEmpty()
    }

    suspend fun removeApiKeyForProvider(providerId: Long) {
        val provider = getProviderById(providerId) ?: return
        provider.encryptedApiKey?.let { encryption.deleteApiKey(it) }
        updateProvider(provider.copy(encryptedApiKey = null))
    }

    // Active provider management methods
    suspend fun getActiveProvider(): LLMProviderEntity? {
        return try {
            val providers = getActiveProviders().first()
            if (providers.isNotEmpty()) providers[0] else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getActiveProviderId(): Long {
        val activeProvider = getActiveProvider()
        return activeProvider?.id ?: -1L
    }

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