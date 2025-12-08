package pro.sihao.jarvis.data.network.dto

data class ModelsResponse(
    val `object`: String,
    val data: List<ModelInfo>
)

data class ModelInfo(
    val id: String,
    val `object`: String,
    val created: Long,
    val owned_by: String
)

data class ModelsErrorResponse(
    val error: ModelsError
)

data class ModelsError(
    val message: String,
    val type: String,
    val code: String?
)