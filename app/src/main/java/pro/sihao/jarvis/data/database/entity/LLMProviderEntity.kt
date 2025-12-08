package pro.sihao.jarvis.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "llm_providers",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["isActive"])
    ]
)
data class LLMProviderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val displayName: String,
    val baseUrl: String,
    val authenticationType: String, // "API_KEY", "BEARER_TOKEN", etc.
    val defaultModel: String?,
    val isActive: Boolean = true,
    val supportsModelDiscovery: Boolean = true,
    val maxTokens: Int? = null,
    val description: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis()
)