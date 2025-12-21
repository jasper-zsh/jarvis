package pro.sihao.jarvis.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pro.sihao.jarvis.core.domain.model.MediaConstants
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PhotoCaptureManager(private val context: Context) {

    private var tempPhotoFile: File? = null

    fun createPhotoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.cacheDir, "temp_photos")
        storageDir.mkdirs()

        return File(storageDir, "JPEG_${timestamp}.jpg")
    }

    fun getCameraIntent(): Intent? {
        return try {
            tempPhotoFile = createPhotoFile()
            val photoURI = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempPhotoFile!!
            )

            Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getGalleryRequest(): PickVisualMediaRequest {
        return PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
            .build()
    }

    suspend fun processCameraResult(): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val file = tempPhotoFile
            if (file == null || !file.exists()) {
                return@withContext Result.failure(Exception("Photo file not found"))
            }

            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: return@withContext Result.failure(Exception("Failed to decode photo"))

            // Clean up temporary file
            file.delete()

            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processGalleryResult(uri: Uri): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                // Get image dimensions
                BitmapFactory.decodeStream(inputStream, null, options)

                // Calculate sample size for efficient loading
                options.inSampleSize = calculateInSampleSize(options)
                options.inJustDecodeBounds = false

                // Reset stream and decode bitmap
                context.contentResolver.openInputStream(uri)?.use { resetStream ->
                    val bitmap = BitmapFactory.decodeStream(resetStream, null, options)
                        ?: return@withContext Result.failure(Exception("Failed to decode gallery image"))

                    Result.success(bitmap)
                } ?: Result.failure(Exception("Failed to reopen image stream"))
            } ?: Result.failure(Exception("Failed to open image stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun compressAndSaveBitmap(bitmap: Bitmap): Result<File> = withContext(Dispatchers.IO) {
        try {
            val compressedBitmap = compressBitmap(bitmap)
            val outputFile = createPhotoFile()

            FileOutputStream(outputFile).use { out ->
                compressedBitmap.compress(
                    MediaConstants.PHOTO_COMPRESSION_FORMAT,
                    MediaConstants.PHOTO_QUALITY,
                    out
                )
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // If image is already within acceptable size, return original
        if (width <= MediaConstants.MAX_PHOTO_WIDTH && height <= MediaConstants.MAX_PHOTO_HEIGHT) {
            return bitmap
        }

        // Calculate new dimensions maintaining aspect ratio
        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = MediaConstants.MAX_PHOTO_WIDTH
            newHeight = (newWidth / aspectRatio).toInt()
        } else {
            newHeight = MediaConstants.MAX_PHOTO_HEIGHT
            newWidth = (newHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > MediaConstants.MAX_PHOTO_HEIGHT || width > MediaConstants.MAX_PHOTO_WIDTH) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= MediaConstants.MAX_PHOTO_HEIGHT &&
                   halfWidth / inSampleSize >= MediaConstants.MAX_PHOTO_WIDTH) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    fun cleanup() {
        // Clean up any temporary files
        tempPhotoFile?.takeIf { it.exists() }?.delete()
        tempPhotoFile = null

        // Clean up old temporary photos
        val tempDir = File(context.cacheDir, "temp_photos")
        if (tempDir.exists()) {
            val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours
            tempDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        }
    }

    fun validateImageSize(fileSizeBytes: Long): Boolean {
        return fileSizeBytes <= MediaConstants.MAX_PHOTO_SIZE_BYTES
    }

    fun getSupportedImageFormats(): List<String> {
        return listOf("image/jpeg", "image/png", "image/webp")
    }
}