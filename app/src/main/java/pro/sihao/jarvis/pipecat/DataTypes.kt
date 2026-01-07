package pro.sihao.jarvis.pipecat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MessageWithType(
    @SerialName("___type___") val type: String
)

@Serializable
data class AudioFrame(
    val audio: String,
    @SerialName("sample_rate") val sampleRate: Int,
    @SerialName("num_channels") val numChannels: Int,
    @SerialName("___type___") val type: String = "audio"
)

@Serializable
data class MessageFrame(
    val message: JsonElement,
    @SerialName("___type___") val type: String = "message"
)