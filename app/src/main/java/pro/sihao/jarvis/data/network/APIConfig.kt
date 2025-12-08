package pro.sihao.jarvis.data.network

/**
 * Configuration for OpenAI-compatible API endpoints
 */
data class APIConfig(
    val baseUrl: String,
    val defaultModel: String,
    val name: String
) {
    companion object {
        // Predefined configurations for popular OpenAI-compatible APIs

        // Official OpenAI API
        val OPENAI = APIConfig(
            baseUrl = "https://api.openai.com/v1/",
            defaultModel = "gpt-3.5-turbo",
            name = "OpenAI"
        )

        // DeepSeek API (OpenAI-compatible)
        val DEEPSEEK = APIConfig(
            baseUrl = "https://api.deepseek.com/v1/",
            defaultModel = "deepseek-chat",
            name = "DeepSeek"
        )

        // Local AI server (like Ollama, LocalAI, etc.)
        val LOCAL_AI = APIConfig(
            baseUrl = "http://localhost:8080/v1/",
            defaultModel = "llama2",
            name = "Local AI"
        )

        // Together.ai (OpenAI-compatible)
        val TOGETHER_AI = APIConfig(
            baseUrl = "https://api.together.xyz/v1/",
            defaultModel = "meta-llama/Llama-2-7b-chat-hf",
            name = "Together.ai"
        )

        // Groq (OpenAI-compatible)
        val GROQ = APIConfig(
            baseUrl = "https://api.groq.com/openai/v1/",
            defaultModel = "llama3-8b-8192",
            name = "Groq"
        )
    }
}