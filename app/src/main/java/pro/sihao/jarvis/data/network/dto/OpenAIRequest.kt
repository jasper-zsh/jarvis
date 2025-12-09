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
    val content: List<ContentPart>
)

data class ContentPart(
    val type: String,
    val text: String? = null,
    val input_audio: InputAudioPayload? = null
)

data class InputAudioPayload(
    val data: String,
    val format: String
)
