package pro.sihao.jarvis.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val timestamp: Long,
    val isFromUser: Boolean,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val contentType: String = "TEXT", // Store as string for Room compatibility
    val mediaUrl: String? = null,
    val duration: Long? = null, // For voice messages in milliseconds
    val thumbnailUrl: String? = null, // For photo thumbnails
    val mediaSize: Long? = null // File size in bytes
)