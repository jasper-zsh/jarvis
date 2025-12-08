package pro.sihao.jarvis.data.network.dto

data class OpenAIResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage?
)

data class OpenAIChoice(
    val index: Int,
    val message: OpenAIMessage,
    val finish_reason: String
)

data class OpenAIUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class OpenAIErrorResponse(
    val error: OpenAIError
)

data class OpenAIError(
    val message: String,
    val type: String,
    val code: String?
)