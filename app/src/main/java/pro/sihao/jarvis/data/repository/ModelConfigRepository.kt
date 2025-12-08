package pro.sihao.jarvis.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import pro.sihao.jarvis.data.database.dao.ModelConfigDao
import pro.sihao.jarvis.data.database.entity.ModelConfigEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelConfigRepository @Inject constructor(
    private val modelConfigDao: ModelConfigDao
) {

    fun getAllModelConfigs(): Flow<List<ModelConfigEntity>> = modelConfigDao.getAllModelConfigs()

    fun getModelConfigsByProvider(providerId: Long): Flow<List<ModelConfigEntity>> =
        modelConfigDao.getModelConfigsByProvider(providerId)

    fun getActiveModelConfigsByProvider(providerId: Long): Flow<List<ModelConfigEntity>> =
        modelConfigDao.getActiveModelConfigsByProvider(providerId)

    suspend fun getModelConfigById(id: Long): ModelConfigEntity? = modelConfigDao.getModelConfigById(id)

    suspend fun getModelConfigByProviderAndModel(providerId: Long, modelName: String): ModelConfigEntity? =
        modelConfigDao.getModelConfigByProviderAndModel(providerId, modelName)

    suspend fun getDefaultModelConfig(): ModelConfigEntity? = modelConfigDao.getDefaultModelConfig()

    suspend fun insertModelConfig(modelConfig: ModelConfigEntity): Long {
        return modelConfigDao.insertModelConfig(modelConfig.copy(
            updatedTimestamp = System.currentTimeMillis()
        ))
    }

    suspend fun updateModelConfig(modelConfig: ModelConfigEntity) {
        modelConfigDao.updateModelConfig(modelConfig.copy(
            updatedTimestamp = System.currentTimeMillis()
        ))
    }

    suspend fun deleteModelConfig(modelConfig: ModelConfigEntity) {
        modelConfigDao.deleteModelConfig(modelConfig)
    }

    suspend fun deleteModelConfigById(id: Long) {
        modelConfigDao.deleteModelConfigById(id)
    }

    suspend fun deleteModelConfigsByProvider(providerId: Long) {
        modelConfigDao.deleteModelConfigsByProvider(providerId)
    }

    suspend fun setModelConfigActiveStatus(id: Long, isActive: Boolean) {
        modelConfigDao.updateModelConfigActiveStatus(id, isActive)
    }

    suspend fun setDefaultModelForProvider(providerId: Long, modelConfigId: Long) {
        // Clear existing default for this provider
        modelConfigDao.clearDefaultModelForProvider(providerId)
        // Set new default
        modelConfigDao.updateModelConfigDefaultStatus(modelConfigId, true)
    }

    suspend fun setActiveAndDefaultModel(providerId: Long, modelConfigId: Long) {
        setModelConfigActiveStatus(modelConfigId, true)
        setDefaultModelForProvider(providerId, modelConfigId)
    }

    suspend fun getModelConfigCountByProvider(providerId: Long): Int =
        modelConfigDao.getModelConfigCountByProvider(providerId)

    suspend fun getActiveModelConfigCountByProvider(providerId: Long): Int =
        modelConfigDao.getActiveModelConfigCountByProvider(providerId)

    // Convenience methods for model configuration
    fun getActiveModelsForProvider(providerId: Long): Flow<List<ModelConfiguration>> {
        return getActiveModelConfigsByProvider(providerId).map { configs ->
            configs.map { config ->
                ModelConfiguration(
                    id = config.id,
                    providerId = config.providerId,
                    modelName = config.modelName,
                    displayName = config.displayName,
                    maxTokens = config.maxTokens,
                    contextWindow = config.contextWindow,
                    inputCostPer1K = config.inputCostPer1K,
                    outputCostPer1K = config.outputCostPer1K,
                    temperature = config.temperature,
                    topP = config.topP,
                    isActive = config.isActive,
                    isDefault = config.isDefault,
                    description = config.description,
                    capabilities = config.capabilities
                )
            }
        }
    }

    suspend fun getDefaultModelForProvider(providerId: Long): ModelConfiguration? {
        return try {
            val defaultEntity = modelConfigDao.getDefaultModelForProvider(providerId)
            defaultEntity?.let { entity ->
                ModelConfiguration(
                    id = entity.id,
                    providerId = entity.providerId,
                    modelName = entity.modelName,
                    displayName = entity.displayName,
                    maxTokens = entity.maxTokens,
                    contextWindow = entity.contextWindow,
                    inputCostPer1K = entity.inputCostPer1K,
                    outputCostPer1K = entity.outputCostPer1K,
                    temperature = entity.temperature,
                    topP = entity.topP,
                    isActive = entity.isActive,
                    isDefault = entity.isDefault,
                    description = entity.description,
                    capabilities = entity.capabilities
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFirstActiveModel(): ModelConfiguration? {
        return try {
            val firstActiveEntity = modelConfigDao.getFirstActiveModel()
            firstActiveEntity?.let { entity ->
                ModelConfiguration(
                    id = entity.id,
                    providerId = entity.providerId,
                    modelName = entity.modelName,
                    displayName = entity.displayName,
                    maxTokens = entity.maxTokens,
                    contextWindow = entity.contextWindow,
                    inputCostPer1K = entity.inputCostPer1K,
                    outputCostPer1K = entity.outputCostPer1K,
                    temperature = entity.temperature,
                    topP = entity.topP,
                    isActive = entity.isActive,
                    isDefault = entity.isDefault,
                    description = entity.description,
                    capabilities = entity.capabilities
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}

data class ModelConfiguration(
    val id: Long,
    val providerId: Long,
    val modelName: String,
    val displayName: String,
    val maxTokens: Int?,
    val contextWindow: Int?,
    val inputCostPer1K: Double?,
    val outputCostPer1K: Double?,
    val temperature: Float,
    val topP: Float,
    val isActive: Boolean,
    val isDefault: Boolean,
    val description: String?,
    val capabilities: List<String>
)