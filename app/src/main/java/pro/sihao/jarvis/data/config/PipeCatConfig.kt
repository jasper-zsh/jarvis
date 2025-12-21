package pro.sihao.jarvis.data.config

/**
 * Environment-based configuration for PipeCat service
 */
object PipeCatConfig {

    // Default development environment
    // TODO: Replace with proper BuildConfig.DEBUG when available
    private val isDevelopment = true // Assume debug for now

    // Base URLs for different environments
    private val DEV_BASE_URL = "http://localhost:7860"
    private val PROD_BASE_URL = "https://your-production-server.com"

    // Default bot IDs
    private val DEV_BOT_ID = "jarvis-dev-assistant"
    private val PROD_BOT_ID = "jarvis-assistant"

    // Get base URL based on build type
    val defaultBaseUrl: String
        get() = if (isDevelopment) DEV_BASE_URL else PROD_BASE_URL

    // Get default bot ID based on build type
    val defaultBotId: String
        get() = if (isDevelopment) DEV_BOT_ID else PROD_BOT_ID

    // Default connection settings
    val defaultEnableMic: Boolean = true
    val defaultEnableCam: Boolean = false

    // Connection timeouts and retries
    const val CONNECTION_TIMEOUT_MS = 30000L
    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 2000L

    // Audio settings
    const val SAMPLE_RATE = 16000
    const val CHANNELS = 1
    const val BIT_RATE = 128000
}