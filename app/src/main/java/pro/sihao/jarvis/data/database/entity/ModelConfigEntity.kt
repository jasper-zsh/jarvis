package pro.sihao.jarvis.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "model_configs",
    foreignKeys = [
        ForeignKey(
            entity = LLMProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["providerId", "modelName"], unique = true),
        Index(value = ["isActive"]),
        Index(value = ["isDefault"])
    ]
)
data class ModelConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val providerId: Long,
    val modelName: String,
    val displayName: String,
    val maxTokens: Int?,
    val contextWindow: Int?,
    val inputCostPer1K: Double?, // Cost per 1K input tokens
    val outputCostPer1K: Double?, // Cost per 1K output tokens
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val description: String? = null,
    val capabilities: List<String> = emptyList(), // JSON string of capabilities
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis()
)