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
}