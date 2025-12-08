package pro.sihao.jarvis.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "jarvis_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val API_KEY = "api_key"
        private const val API_PROVIDER = "api_provider"
        private const val BASE_URL = "base_url"
        private const val MODEL_NAME = "model_name"
        private const val TEMPERATURE = "temperature"
        private const val MAX_TOKENS = "max_tokens"

        // Provider-specific API keys
        private const val API_KEY_PREFIX = "api_key_"

        // Active selections
        private const val ACTIVE_PROVIDER_ID = "active_provider_id"
        private const val ACTIVE_MODEL_CONFIG_ID = "active_model_config_id"
    }

    // API Key methods (now provider-agnostic)
    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit()
            .putString(API_KEY, apiKey)
            .apply()
    }

    fun getApiKey(): String? {
        return sharedPreferences.getString(API_KEY, null)
    }

    fun removeApiKey() {
        sharedPreferences.edit()
            .remove(API_KEY)
            .apply()
    }

    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrEmpty()
    }

    // Legacy OpenAI methods for backward compatibility
    fun saveOpenAIApiKey(apiKey: String) = saveApiKey(apiKey)
    fun getOpenAIApiKey() = getApiKey()
    fun removeOpenAIApiKey() = removeApiKey()

    // API Provider methods
    fun saveApiProvider(provider: String) {
        sharedPreferences.edit()
            .putString(API_PROVIDER, provider)
            .apply()
    }

    fun getApiProvider(): String {
        return sharedPreferences.getString(API_PROVIDER, "OPENAI") ?: "OPENAI"
    }

    // Base URL methods
    fun saveBaseUrl(baseUrl: String) {
        sharedPreferences.edit()
            .putString(BASE_URL, baseUrl)
            .apply()
    }

    fun getBaseUrl(): String {
        return sharedPreferences.getString(BASE_URL, "") ?: ""
    }

    fun hasCustomBaseUrl(): Boolean {
        val baseUrl = getBaseUrl()
        return baseUrl.isNotBlank()
    }

    // Model configuration methods
    fun saveModelName(modelName: String) {
        sharedPreferences.edit()
            .putString(MODEL_NAME, modelName)
            .apply()
    }

    fun getModelName(): String {
        return sharedPreferences.getString(MODEL_NAME, "") ?: ""
    }

    fun saveTemperature(temperature: Float) {
        sharedPreferences.edit()
            .putFloat(TEMPERATURE, temperature)
            .apply()
    }

    fun getTemperature(): Float {
        return sharedPreferences.getFloat(TEMPERATURE, 0.7f)
    }

    fun saveMaxTokens(maxTokens: Int) {
        sharedPreferences.edit()
            .putInt(MAX_TOKENS, maxTokens)
            .apply()
    }

    fun getMaxTokens(): Int {
        return sharedPreferences.getInt(MAX_TOKENS, 1000)
    }

    // Provider-specific API key methods
    fun saveApiKeyForProvider(providerId: Long, apiKey: String) {
        sharedPreferences.edit()
            .putString("${API_KEY_PREFIX}${providerId}", apiKey)
            .apply()
    }

    fun getApiKeyForProvider(providerId: Long): String? {
        return sharedPreferences.getString("${API_KEY_PREFIX}${providerId}", null)
    }

    fun removeApiKeyForProvider(providerId: Long) {
        sharedPreferences.edit()
            .remove("${API_KEY_PREFIX}${providerId}")
            .apply()
    }

    fun hasApiKeyForProvider(providerId: Long): Boolean {
        return !getApiKeyForProvider(providerId).isNullOrEmpty()
    }

    // Active selection methods (coordinate with database)
    fun saveActiveProviderId(providerId: Long) {
        sharedPreferences.edit()
            .putLong(ACTIVE_PROVIDER_ID, providerId)
            .apply()
    }

    fun getActiveProviderId(): Long {
        return sharedPreferences.getLong(ACTIVE_PROVIDER_ID, -1)
    }

    fun removeActiveProviderId() {
        sharedPreferences.edit()
            .remove(ACTIVE_PROVIDER_ID)
            .apply()
    }

    fun hasActiveProviderId(): Boolean {
        return getActiveProviderId() != -1L
    }

    fun saveActiveModelConfigId(modelConfigId: Long) {
        sharedPreferences.edit()
            .putLong(ACTIVE_MODEL_CONFIG_ID, modelConfigId)
            .apply()
    }

    fun getActiveModelConfigId(): Long {
        return sharedPreferences.getLong(ACTIVE_MODEL_CONFIG_ID, -1)
    }

    fun removeActiveModelConfigId() {
        sharedPreferences.edit()
            .remove(ACTIVE_MODEL_CONFIG_ID)
            .apply()
    }

    fun hasActiveModelConfigId(): Boolean {
        return getActiveModelConfigId() != -1L
    }

    // Migration and cleanup methods
    fun clearLegacySettings() {
        sharedPreferences.edit()
            .remove(API_PROVIDER)
            .remove(BASE_URL)
            .remove(MODEL_NAME)
            .remove(TEMPERATURE)
            .remove(MAX_TOKENS)
            .apply()
    }

    fun isLegacySettingsPresent(): Boolean {
        return !sharedPreferences.getString(API_PROVIDER, null).isNullOrEmpty() ||
               !sharedPreferences.getString(BASE_URL, null).isNullOrEmpty() ||
               !sharedPreferences.getString(MODEL_NAME, null).isNullOrEmpty()
    }
}