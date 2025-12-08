package pro.sihao.jarvis.data.network.dto

data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<OpenAIMessage>,
    val stream: Boolean = false,
    val temperature: Float = 0.7f,
    val max_tokens: Int? = null
)

data class OpenAIMessage(
    val role: String,
    val content: String
)