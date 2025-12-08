package pro.sihao.jarvis.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pro.sihao.jarvis.data.database.entity.ModelConfigEntity

@Dao
interface ModelConfigDao {

    @Query("SELECT * FROM model_configs ORDER BY providerId, modelName")
    fun getAllModelConfigs(): Flow<List<ModelConfigEntity>>

    @Query("SELECT * FROM model_configs WHERE providerId = :providerId ORDER BY modelName")
    fun getModelConfigsByProvider(providerId: Long): Flow<List<ModelConfigEntity>>

    @Query("SELECT * FROM model_configs WHERE providerId = :providerId AND isActive = 1 ORDER BY modelName")
    fun getActiveModelConfigsByProvider(providerId: Long): Flow<List<ModelConfigEntity>>

    @Query("SELECT * FROM model_configs WHERE id = :id")
    suspend fun getModelConfigById(id: Long): ModelConfigEntity?

    @Query("SELECT * FROM model_configs WHERE providerId = :providerId AND modelName = :modelName")
    suspend fun getModelConfigByProviderAndModel(providerId: Long, modelName: String): ModelConfigEntity?

    @Query("SELECT * FROM model_configs WHERE isDefault = 1 AND isActive = 1 LIMIT 1")
    suspend fun getDefaultModelConfig(): ModelConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModelConfig(modelConfig: ModelConfigEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModelConfigs(modelConfigs: List<ModelConfigEntity>): List<Long>

    @Update
    suspend fun updateModelConfig(modelConfig: ModelConfigEntity)

    @Delete
    suspend fun deleteModelConfig(modelConfig: ModelConfigEntity)

    @Query("DELETE FROM model_configs WHERE id = :id")
    suspend fun deleteModelConfigById(id: Long)

    @Query("DELETE FROM model_configs WHERE providerId = :providerId")
    suspend fun deleteModelConfigsByProvider(providerId: Long)

    @Query("UPDATE model_configs SET isActive = :isActive WHERE id = :id")
    suspend fun updateModelConfigActiveStatus(id: Long, isActive: Boolean)

    @Query("UPDATE model_configs SET isDefault = 0 WHERE providerId = :providerId")
    suspend fun clearDefaultModelForProvider(providerId: Long)

    @Query("UPDATE model_configs SET isDefault = :isDefault WHERE id = :id")
    suspend fun updateModelConfigDefaultStatus(id: Long, isDefault: Boolean)

    @Query("SELECT COUNT(*) FROM model_configs WHERE providerId = :providerId")
    suspend fun getModelConfigCountByProvider(providerId: Long): Int

    @Query("SELECT COUNT(*) FROM model_configs WHERE providerId = :providerId AND isActive = 1")
    suspend fun getActiveModelConfigCountByProvider(providerId: Long): Int

    @Query("SELECT * FROM model_configs WHERE providerId = :providerId AND isDefault = 1 AND isActive = 1 LIMIT 1")
    suspend fun getDefaultModelForProvider(providerId: Long): ModelConfigEntity?

    @Query("SELECT * FROM model_configs WHERE isActive = 1 ORDER BY providerId, modelName LIMIT 1")
    suspend fun getFirstActiveModel(): ModelConfigEntity?
}