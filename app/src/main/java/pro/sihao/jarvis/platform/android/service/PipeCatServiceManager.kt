package pro.sihao.jarvis.platform.android.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pro.sihao.jarvis.core.domain.model.PipeCatConfig
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.core.domain.service.PipeCatService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for PipeCat foreground service operations
 *
 * This class provides the interface for the UI to interact with the PipeCat foreground service.
 * It handles starting/stopping the service and ensures proper permission handling.
 */
@Singleton
class PipeCatServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pipeCatService: PipeCatService
) {
    companion object {
        private const val TAG = "PipeCatServiceManager"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    /**
     * 启动常驻PipeCat服务
     */
    fun startPersistentService() {
        try {
            Log.d(TAG, "启动常驻PipeCat服务")

            val intent = Intent(context, PipeCatForegroundService::class.java).apply {
                action = PipeCatForegroundService.ACTION_START_SERVICE
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "使用ContextCompat.startForegroundService启动常驻服务")
                ContextCompat.startForegroundService(context, intent)
            } else {
                Log.d(TAG, "使用startService启动常驻服务")
                context.startService(intent)
            }

            Log.d(TAG, "常驻PipeCat服务启动命令发送成功")
        } catch (e: Exception) {
            Log.e(TAG, "启动常驻PipeCat服务失败", e)
            throw e
        }
    }

    /**
     * 激活PipeCat会话
     */
    fun activateSession() {
        try {
            Log.d(TAG, "激活PipeCat会话")

            val intent = Intent(context, PipeCatForegroundService::class.java).apply {
                action = PipeCatForegroundService.ACTION_ACTIVATE_SESSION
            }
            context.startService(intent)

            Log.d(TAG, "激活PipeCat会话命令发送成功")
        } catch (e: Exception) {
            Log.e(TAG, "激活PipeCat会话失败", e)
        }
    }

    /**
     * 停用PipeCat会话
     */
    fun deactivateSession() {
        try {
            Log.d(TAG, "停用PipeCat会话")

            val intent = Intent(context, PipeCatForegroundService::class.java).apply {
                action = PipeCatForegroundService.ACTION_DEACTIVATE_SESSION
            }
            context.startService(intent)

            Log.d(TAG, "停用PipeCat会话命令发送成功")
        } catch (e: Exception) {
            Log.e(TAG, "停用PipeCat会话失败", e)
        }
    }

  
    /**
     * Stop the PipeCat foreground service
     */
    fun stopService() {
        try {
            Log.d(TAG, "Stopping PipeCat foreground service")

            val intent = Intent(context, PipeCatForegroundService::class.java).apply {
                action = PipeCatForegroundService.ACTION_STOP_SERVICE
            }
            context.startService(intent)

            Log.d(TAG, "PipeCat foreground service stop command sent")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping PipeCat foreground service", e)
        }
    }

    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required before Android 13
        }
    }

    /**
     * Request notification permission from activity (Android 13+)
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * Check if foreground service permissions are granted
     */
    fun hasForegroundServicePermissions(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.FOREGROUND_SERVICE,
                android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                android.Manifest.permission.FOREGROUND_SERVICE,
                android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
                android.Manifest.permission.RECORD_AUDIO
            )
        }

        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request foreground service permissions from activity
     */
    fun requestForegroundServicePermissions(activity: Activity) {
        val missingPermissions = mutableListOf<String>()

        // Always required permissions
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(android.Manifest.permission.FOREGROUND_SERVICE)
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(android.Manifest.permission.RECORD_AUDIO)
        }

        // Android 13+ notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * Get the PipeCat connection state from the service
     */
    val connectionState: StateFlow<PipeCatConnectionState>
        get() = pipeCatService.connectionState

    /**
     * 连接到PipeCat (使用常驻服务模式)
     * 现在只是激活常驻服务中的会话
     */
    suspend fun connect(config: PipeCatConfig) {
        try {
            Log.d(TAG, "连接PipeCat - 激活常驻服务会话")

            // 确保常驻服务已启动
            startPersistentService()

            // 小延迟确保服务准备就绪
            kotlinx.coroutines.delay(500)

            // 激活PipeCat会话（在常驻服务中处理）
            activateSession()

            Log.d(TAG, "PipeCat会话激活请求已发送")
        } catch (e: Exception) {
            Log.e(TAG, "连接PipeCat失败", e)
            throw e
        }
    }

    /**
     * 断开PipeCat连接 (使用常驻服务模式)
     * 现在只是停用常驻服务中的会话
     */
    suspend fun disconnect() {
        try {
            Log.d(TAG, "断开PipeCat - 停用常驻服务会话")

            // 停用PipeCat会话（保持常驻服务运行）
            deactivateSession()

            Log.d(TAG, "PipeCat会话停用请求已发送")
        } catch (e: Exception) {
            Log.e(TAG, "断开PipeCat失败", e)
            throw e
        }
    }

    /**
     * Toggle microphone
     */
    fun toggleMicrophone(enabled: Boolean) {
        try {
            pipeCatService.toggleMicrophone(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling microphone", e)
        }
    }

    /**
     * Toggle camera
     */
    fun toggleCamera(enabled: Boolean) {
        try {
            pipeCatService.toggleCamera(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling camera", e)
        }
    }

    /**
     * Clean up resources when the manager is no longer needed
     */
    fun cleanup() {
        try {
            serviceScope.cancel()
            Log.d(TAG, "PipeCatServiceManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}