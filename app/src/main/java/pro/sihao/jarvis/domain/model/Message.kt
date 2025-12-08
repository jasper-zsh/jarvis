package pro.sihao.jarvis.domain.model

import java.util.Date

data class Message(
    val id: Long = 0,
    val content: String,
    val timestamp: Date,
    val isFromUser: Boolean,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)