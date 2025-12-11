package pro.sihao.jarvis.domain.model

data class RokidGlassesDevice(
    val name: String,
    val macAddress: String?,
    val socketUuid: String? = null,
    val rokidAccount: String? = null,
    val glassesType: Int? = null
)

data class GlassesIntegrationSettings(
    val useGlassesInput: Boolean = false,
    val useGlassesOutput: Boolean = false
)

enum class GlassesConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
