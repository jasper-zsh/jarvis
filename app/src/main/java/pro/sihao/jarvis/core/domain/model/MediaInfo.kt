package pro.sihao.jarvis.core.domain.model

import android.graphics.Bitmap

data class MediaInfo(
    val url: String,
    val size: Long,
    val duration: Long? = null, // For voice messages in milliseconds
    val thumbnailUrl: String? = null, // For photo thumbnails
    val mimeType: String,
    val width: Int? = null, // For photos/videos
    val height: Int? = null // For photos/videos
)

object MediaConstants {
    // Voice message limits
    const val MAX_VOICE_DURATION_MS = 120_000L // 2 minutes
    const val VOICE_SAMPLE_RATE = 44100
    const val VOICE_BIT_RATE = 128_000 // 128 kbps
    const val VOICE_EXTENSION = ".aac"
    const val VOICE_MIME_TYPE = "audio/aac"

    // Photo limits
    const val MAX_PHOTO_SIZE_BYTES = 5_242_880L // 5MB
    const val MAX_PHOTO_WIDTH = 1920
    const val MAX_PHOTO_HEIGHT = 1080
    const val THUMBNAIL_WIDTH = 200
    const val THUMBNAIL_HEIGHT = 200
    const val PHOTO_QUALITY = 85
    const val THUMBNAIL_EXTENSION = "_thumb.jpg"
    const val JPEG_MIME_TYPE = "image/jpeg"
    const val PNG_MIME_TYPE = "image/png"

    // File naming and storage
    const val MEDIA_DIRECTORY = "media"
    const val VOICE_DIRECTORY = "voice"
    const val PHOTO_DIRECTORY = "photos"
    const val THUMBNAIL_DIRECTORY = "thumbnails"

    // Compression
    val PHOTO_COMPRESSION_FORMAT = Bitmap.CompressFormat.JPEG
    val THUMBNAIL_COMPRESSION_FORMAT = Bitmap.CompressFormat.JPEG
}