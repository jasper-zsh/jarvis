package pro.sihao.jarvis.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pro.sihao.jarvis.domain.model.ContentType
import pro.sihao.jarvis.domain.model.MediaConstants
import java.io.*
import java.util.*

class MediaStorageManager(private val context: Context) {

    private val mediaDir = File(context.filesDir, MediaConstants.MEDIA_DIRECTORY)
    private val voiceDir = File(mediaDir, MediaConstants.VOICE_DIRECTORY)
    private val photoDir = File(mediaDir, MediaConstants.PHOTO_DIRECTORY)
    private val thumbnailDir = File(mediaDir, MediaConstants.THUMBNAIL_DIRECTORY)

    init {
        createDirectories()
    }

    private fun createDirectories() {
        mediaDir.mkdirs()
        voiceDir.mkdirs()
        photoDir.mkdirs()
        thumbnailDir.mkdirs()
    }

    fun createVoiceFile(extension: String = "aac"): File {
        val fileName = generateUniqueFileName(ContentType.VOICE, extension)
        return File(voiceDir, fileName)
    }

    fun generateUniqueFileName(contentType: ContentType, extension: String): String {
        val uuid = UUID.randomUUID().toString()
        val ext = extension.takeIf { it.startsWith(".") } ?: ".$extension"
        return when (contentType) {
            ContentType.VOICE -> "$uuid$ext"
            ContentType.PHOTO -> "$uuid.$extension"
            ContentType.TEXT -> "$uuid.txt"
        }
    }

    suspend fun saveVoiceRecording(audioData: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = generateUniqueFileName(ContentType.VOICE, "aac")
            val file = File(voiceDir, fileName)

            FileOutputStream(file).use { output ->
                output.write(audioData)
            }

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePhoto(bitmap: Bitmap): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val fileName = generateUniqueFileName(ContentType.PHOTO, "jpg")
            val photoFile = File(photoDir, fileName)
            val thumbnailFileName = fileName.replace(".jpg", MediaConstants.THUMBNAIL_EXTENSION)
            val thumbnailFile = File(thumbnailDir, thumbnailFileName)

            // Save compressed photo
            val compressedBitmap = compressBitmap(bitmap, MediaConstants.MAX_PHOTO_WIDTH, MediaConstants.MAX_PHOTO_HEIGHT)
            FileOutputStream(photoFile).use { output ->
                compressedBitmap.compress(MediaConstants.PHOTO_COMPRESSION_FORMAT, MediaConstants.PHOTO_QUALITY, output)
            }

            // Generate and save thumbnail
            val thumbnail = createThumbnail(compressedBitmap)
            FileOutputStream(thumbnailFile).use { output ->
                thumbnail.compress(MediaConstants.THUMBNAIL_COMPRESSION_FORMAT, MediaConstants.PHOTO_QUALITY, output)
            }

            Result.success(Pair(photoFile.absolutePath, thumbnailFile.absolutePath))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePhotoFromUri(inputStream: InputStream): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
                ?: return@withContext Result.failure(Exception("Failed to decode image"))

            // Handle EXIF rotation
            val rotatedBitmap = handleImageRotation(originalBitmap, inputStream)
            savePhoto(rotatedBitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMediaFile(contentType: ContentType, fileName: String): File? {
        return when (contentType) {
            ContentType.VOICE -> File(voiceDir, fileName)
            ContentType.PHOTO -> File(photoDir, fileName)
            ContentType.TEXT -> File(mediaDir, fileName)
        }
    }

    fun getThumbnailFile(fileName: String): File? {
        return File(thumbnailDir, fileName)
    }

    fun deleteMedia(contentType: ContentType, fileName: String): Boolean {
        val mediaFile = getMediaFile(contentType, fileName)
        return mediaFile?.delete() ?: false
    }

    fun deleteThumbnail(fileName: String): Boolean {
        val thumbnailFile = getThumbnailFile(fileName)
        return thumbnailFile?.delete() ?: false
    }

    fun deleteFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            val file = File(path)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    fun getMediaSize(contentType: ContentType, fileName: String): Long? {
        val mediaFile = getMediaFile(contentType, fileName)
        return mediaFile?.length()
    }

    fun cleanupOldMedia(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) { // Default: 7 days
        val cutoffTime = System.currentTimeMillis() - maxAgeMs

        voiceDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
            }
        }

        photoDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
            }
        }

        thumbnailDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
            }
        }
    }

    fun getTotalStorageSize(): Long {
        return voiceDir.walkTopDown().sumOf { it.length() } +
               photoDir.walkTopDown().sumOf { it.length() } +
               thumbnailDir.walkTopDown().sumOf { it.length() }
    }

    private fun compressBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(
            bitmap,
            MediaConstants.THUMBNAIL_WIDTH,
            MediaConstants.THUMBNAIL_HEIGHT,
            true
        )
    }

    private fun handleImageRotation(bitmap: Bitmap, inputStream: InputStream): Bitmap {
        return try {
            // Note: This is a simplified version. In production, you'd want to save the InputStream to a temporary file first
            // to properly read EXIF data, as ExifInterface requires a file path or FileDescriptor.
            val matrix = Matrix()
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            // Recycle the original bitmap if we created a new one
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }

            rotatedBitmap
        } catch (e: Exception) {
            // Fallback to original bitmap if EXIF handling fails
            bitmap
        }
    }
}
