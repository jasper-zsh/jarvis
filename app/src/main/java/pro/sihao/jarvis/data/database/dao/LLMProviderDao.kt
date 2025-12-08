package pro.sihao.jarvis.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pro.sihao.jarvis.data.database.entity.LLMProviderEntity

@Dao
interface LLMProviderDao {

    @Query("SELECT * FROM llm_providers ORDER BY name")
    fun getAllProviders(): Flow<List<LLMProviderEntity>>

    @Query("SELECT * FROM llm_providers WHERE isActive = 1 ORDER BY name")
    fun getActiveProviders(): Flow<List<LLMProviderEntity>>

    @Query("SELECT * FROM llm_providers WHERE id = :id")
    suspend fun getProviderById(id: Long): LLMProviderEntity?

    @Query("SELECT * FROM llm_providers WHERE name = :name")
    suspend fun getProviderByName(name: String): LLMProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: LLMProviderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviders(providers: List<LLMProviderEntity>): List<Long>

    @Update
    suspend fun updateProvider(provider: LLMProviderEntity)

    @Delete
    suspend fun deleteProvider(provider: LLMProviderEntity)

    @Query("DELETE FROM llm_providers WHERE id = :id")
    suspend fun deleteProviderById(id: Long)

    @Query("UPDATE llm_providers SET isActive = :isActive WHERE id = :id")
    suspend fun updateProviderActiveStatus(id: Long, isActive: Boolean)

    @Query("UPDATE llm_providers SET isActive = 0 WHERE id != :activeId")
    suspend fun setAllProvidersInactiveExcept(activeId: Long)

    @Query("SELECT COUNT(*) FROM llm_providers")
    suspend fun getProviderCount(): Int

    @Query("SELECT COUNT(*) FROM llm_providers WHERE isActive = 1")
    suspend fun getActiveProviderCount(): Int
}