package pro.sihao.jarvis.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pro.sihao.jarvis.data.repository.ProviderRepository
import pro.sihao.jarvis.data.storage.SecureStorage
import pro.sihao.jarvis.data.network.api.OpenAICompatibleApiService
import pro.sihao.jarvis.data.network.dto.ModelsResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of provider health check
 */
data class ProviderHealthResult(
    val providerId: Long,
    val providerName: String,
    val isHealthy: Boolean,
    val responseTime: Long, // in milliseconds
    val error: String? = null,
    val availableModels: List<String>? = null
)

/**
 * Service for checking provider health and connectivity
 */
@Singleton
class ProviderHealthService @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val secureStorage: SecureStorage
) {

    suspend fun checkProviderHealth(providerId: Long): ProviderHealthResult = withContext(Dispatchers.IO) {
        val provider = providerRepository.getProviderById(providerId)

        if (provider == null) {
            return@withContext ProviderHealthResult(
                providerId = providerId,
                providerName = "Unknown",
                isHealthy = false,
                responseTime = 0L,
                error = "Provider not found"
            )
        }

        if (!provider.isActive) {
            return@withContext ProviderHealthResult(
                providerId = providerId,
                providerName = provider.name,
                isHealthy = false,
                responseTime = 0L,
                error = "Provider is inactive"
            )
        }

        val startTime = System.currentTimeMillis()

        try {
            // Get API key
            val apiKey = secureStorage.getApiKeyForProvider(providerId)
            if (apiKey.isNullOrEmpty()) {
                return@withContext ProviderHealthResult(
                    providerId = providerId,
                    providerName = provider.name,
                    isHealthy = false,
                    responseTime = 0L,
                    error = "No API key configured"
                )
            }

            // Create API service with timeout
            val retrofit = Retrofit.Builder()
                .baseUrl(provider.baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(
                    OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .build()
                )
                .build()

            val apiService = retrofit.create(OpenAICompatibleApiService::class.java)

            // Try to list models as a health check
            val response = apiService.listModels("Bearer $apiKey")
            val responseTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                val responseBody = response.body()
                val models = responseBody?.data?.map { it.id } ?: emptyList()

                ProviderHealthResult(
                    providerId = providerId,
                    providerName = provider.name,
                    isHealthy = true,
                    responseTime = responseTime,
                    availableModels = models
                )
            } else {
                val errorBody = response.errorBody()?.string()
                ProviderHealthResult(
                    providerId = providerId,
                    providerName = provider.name,
                    isHealthy = false,
                    responseTime = responseTime,
                    error = "API Error: ${response.code()} - ${errorBody ?: "Unknown error"}"
                )
            }

        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            ProviderHealthResult(
                providerId = providerId,
                providerName = provider.name,
                isHealthy = false,
                responseTime = responseTime,
                error = e.message ?: "Unknown error occurred"
            )
        }
    }

    suspend fun checkAllProvidersHealth(): List<ProviderHealthResult> = withContext(Dispatchers.IO) {
        val activeProviders = providerRepository.getActiveProviders().first()

        activeProviders.map { provider ->
            async {
                checkProviderHealth(provider.id)
            }
        }.awaitAll()
    }
}