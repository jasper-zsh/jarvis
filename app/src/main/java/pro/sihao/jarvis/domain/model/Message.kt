package pro.sihao.jarvis.domain.model

import java.util.Date

enum class ContentType {
    TEXT,
    VOICE,
    PHOTO,
    REALTIME_TRANSCRIPT,
    REALTIME_RESPONSE
}

data class Message(
    val id: Long = 0,
    val content: String,
    val timestamp: Date,
    val isFromUser: Boolean,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val contentType: ContentType = ContentType.TEXT,
    val mediaUrl: String? = null,
    val duration: Long? = null, // For voice messages in milliseconds
    val thumbnailUrl: String? = null, // For photo thumbnails
    val mediaSize: Long? = null // File size in bytes
)