package pro.sihao.jarvis.data.config

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pro.sihao.jarvis.data.encryption.ApikeyEncryption
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages PipeCat configuration with secure storage and environment-based defaults
 */
@Singleton
class ConfigurationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryption: ApikeyEncryption
) {

    companion object {
        private const val PREFS_NAME = "pipecat_config"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BOT_ID = "bot_id"
        private const val KEY_ENABLE_MIC = "enable_mic"
        private const val KEY_ENABLE_CAM = "enable_cam"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Configuration state flows
    private val _baseUrl = MutableStateFlow(getBaseUrl())
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _apiKey = MutableStateFlow(getApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _botId = MutableStateFlow(getBotId())
    val botId: StateFlow<String> = _botId.asStateFlow()

    private val _enableMic = MutableStateFlow(getEnableMic())
    val enableMic: StateFlow<Boolean> = _enableMic.asStateFlow()

    private val _enableCam = MutableStateFlow(getEnableCam())
    val enableCam: StateFlow<Boolean> = _enableCam.asStateFlow()

    // Getters with fallback to defaults
    private fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, null) ?: PipeCatConfig.defaultBaseUrl
    }

    private fun getApiKey(): String {
        return encryption.getApiKey() ?: ""
    }

    private fun getBotId(): String {
        return prefs.getString(KEY_BOT_ID, null) ?: PipeCatConfig.defaultBotId
    }

    private fun getEnableMic(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_MIC, PipeCatConfig.defaultEnableMic)
    }

    private fun getEnableCam(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_CAM, PipeCatConfig.defaultEnableCam)
    }

    // Setters with secure storage
    fun setBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url.trim()).apply()
        _baseUrl.value = url.trim()
    }

    fun setApiKey(key: String) {
        encryption.saveApiKey(key.trim())
        _apiKey.value = key.trim()
    }

    fun setBotId(id: String) {
        prefs.edit().putString(KEY_BOT_ID, id.trim()).apply()
        _botId.value = id.trim()
    }

    fun setEnableMic(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_MIC, enabled).apply()
        _enableMic.value = enabled
    }

    fun setEnableCam(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_CAM, enabled).apply()
        _enableCam.value = enabled
    }

    // Get current configuration as object
    fun getCurrentConfig(): pro.sihao.jarvis.domain.model.PipeCatConfig {
        return pro.sihao.jarvis.domain.model.PipeCatConfig(
            enableMic = _enableMic.value,
            enableCam = _enableCam.value,
            botId = _botId.value,
            baseUrl = _baseUrl.value,
            apiKey = _apiKey.value.takeIf { it.isNotEmpty() },
            customHeaders = buildMap {
                _apiKey.value.takeIf { it.isNotEmpty() }?.let {
                    put("Authorization", "Bearer $it")
                }
            }
        )
    }

    // Reset to defaults
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        encryption.clearApiKey()

        _baseUrl.value = PipeCatConfig.defaultBaseUrl
        _apiKey.value = ""
        _botId.value = PipeCatConfig.defaultBotId
        _enableMic.value = PipeCatConfig.defaultEnableMic
        _enableCam.value = PipeCatConfig.defaultEnableCam
    }

    // Validate current configuration
    fun validateConfiguration(): ValidationResult {
        return when {
            _baseUrl.value.isBlank() -> ValidationResult(false, "Base URL is required")
            !_baseUrl.value.startsWith("http") -> ValidationResult(false, "Invalid base URL format - must start with http:// or https://")
            _botId.value.isBlank() -> ValidationResult(false, "Bot ID is required")
            else -> ValidationResult(true, "Configuration is valid")
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}