package pro.sihao.jarvis.data.network.dto

data class StreamChunk(
    val choices: List<StreamChoice>
)

data class StreamChoice(
    val delta: StreamDelta?
)

data class StreamDelta(
    val content: List<ContentPart>?
)
