package pro.sihao.jarvis.platform.android.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    enum class PermissionStatus {
        GRANTED,
        DENIED,
        PERMANENTLY_DENIED,
        NOT_REQUIRED
    }

    // Voice recording permissions
    fun getVoiceRecordingPermissionStatus(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activity = context as? Activity
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                    PermissionStatus.GRANTED
                }
                activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                ) -> {
                    PermissionStatus.DENIED
                }
                activity == null -> PermissionStatus.DENIED
                else -> {
                    PermissionStatus.PERMANENTLY_DENIED
                }
            }
        } else {
            PermissionStatus.NOT_REQUIRED
        }
    }

    // Camera permissions
    fun getCameraPermissionStatus(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activity = context as? Activity
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                    PermissionStatus.GRANTED
                }
                activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.CAMERA
                ) -> {
                    PermissionStatus.DENIED
                }
                activity == null -> PermissionStatus.DENIED
                else -> {
                    PermissionStatus.PERMANENTLY_DENIED
                }
            }
        } else {
            PermissionStatus.NOT_REQUIRED
        }
    }

    // Storage permissions for photo gallery
    fun getStoragePermissionStatus(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES
            val activity = context as? Activity
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED -> {
                    PermissionStatus.GRANTED
                }
                activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) -> {
                    PermissionStatus.DENIED
                }
                activity == null -> PermissionStatus.DENIED
                else -> {
                    PermissionStatus.PERMANENTLY_DENIED
                }
            }
        } else {
            // Android 12 and below uses READ_EXTERNAL_STORAGE
            val activity = context as? Activity
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                    PermissionStatus.GRANTED
                }
                activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) -> {
                    PermissionStatus.DENIED
                }
                activity == null -> PermissionStatus.DENIED
                else -> {
                    PermissionStatus.PERMANENTLY_DENIED
                }
            }
        }
    }

    // Get all permissions for voice recording
    fun getVoiceRecordingPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Get all permissions for camera capture
    fun getCameraPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // Get all permissions for photo gallery
    fun getGalleryPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Request voice recording permissions
    fun requestVoiceRecordingPermissions(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(getVoiceRecordingPermissions())
    }

    // Request camera permissions
    fun requestCameraPermissions(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(getCameraPermissions())
    }

    // Request gallery permissions
    fun requestGalleryPermissions(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(getGalleryPermissions())
    }

    // Check if all required permissions are granted for voice recording
    fun hasVoiceRecordingPermissions(): Boolean {
        return getVoiceRecordingPermissionStatus() == PermissionStatus.GRANTED
    }

    // Check if all required permissions are granted for camera
    fun hasCameraPermissions(): Boolean {
        return getCameraPermissionStatus() == PermissionStatus.GRANTED
    }

    // Check if all required permissions are granted for gallery
    fun hasGalleryPermissions(): Boolean {
        return getStoragePermissionStatus() == PermissionStatus.GRANTED
    }

    // Open app settings for permission management
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // Get permission rationale messages
    fun getVoiceRecordingRationale(): String {
        return "Jarvis needs microphone access to record voice messages. This allows you to send voice recordings instead of typing."
    }

    fun getCameraRationale(): String {
        return "Jarvis needs camera access to take photos. This allows you to capture and share images in your conversations."
    }

    fun getGalleryRationale(): String {
        return "Jarvis needs access to your photos to select images from your gallery. This allows you to share existing photos in your conversations."
    }

    // Get permission denied messages
    fun getVoiceRecordingDeniedMessage(): String {
        return "Microphone access was denied. You can enable it in Settings if you change your mind."
    }

    fun getCameraDeniedMessage(): String {
        return "Camera access was denied. You can enable it in Settings if you change your mind."
    }

    fun getGalleryDeniedMessage(): String {
        return "Photo access was denied. You can enable it in Settings if you change your mind."
    }

    // Check if hardware is available
    fun hasMicrophoneHardware(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    }

    fun hasCameraHardware(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    // Get comprehensive status for all media permissions
    fun getMediaPermissionsSummary(): MediaPermissionsSummary {
        return MediaPermissionsSummary(
            voiceRecordingStatus = getVoiceRecordingPermissionStatus(),
            cameraStatus = getCameraPermissionStatus(),
            galleryStatus = getStoragePermissionStatus(),
            hasMicrophoneHardware = hasMicrophoneHardware(),
            hasCameraHardware = hasCameraHardware()
        )
    }

    data class MediaPermissionsSummary(
        val voiceRecordingStatus: PermissionStatus,
        val cameraStatus: PermissionStatus,
        val galleryStatus: PermissionStatus,
        val hasMicrophoneHardware: Boolean,
        val hasCameraHardware: Boolean
    ) {
        val canRecordVoice: Boolean
            get() = hasMicrophoneHardware && voiceRecordingStatus != PermissionStatus.PERMANENTLY_DENIED

        val canTakePhoto: Boolean
            get() = hasCameraHardware && cameraStatus != PermissionStatus.PERMANENTLY_DENIED

        val canSelectFromGallery: Boolean
            get() = galleryStatus != PermissionStatus.PERMANENTLY_DENIED

        val hasAllPermissions: Boolean
            get() = canRecordVoice && canTakePhoto && canSelectFromGallery
    }
}
