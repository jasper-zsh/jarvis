package pro.sihao.jarvis.platform.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.rokid.cxr.client.extend.CxrApi
import com.rokid.cxr.client.extend.listeners.AudioStreamListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pro.sihao.jarvis.MainActivity
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.core.domain.model.PipeCatConfig
import pro.sihao.jarvis.core.domain.model.PipeCatEvent
import pro.sihao.jarvis.core.domain.service.PipeCatService
import pro.sihao.jarvis.features.realtime.data.config.ConfigurationManager
import pro.sihao.jarvis.features.realtime.data.service.PipeCatServiceImpl
import pro.sihao.jarvis.platform.network.webrtc.PipeCatConnectionManager
import javax.inject.Inject

/**
 * 常驻前台服务，用于托管PipeCat连接和glasses集成
 *
 * 设计原则：
 * 1. 应用启动时自动启动，保持常驻
 * 2. 智能资源管理，按需激活PipeCat会话
 * 3. 持续监听glasses状态，提供无缝语音交互
 * 4. 优化通知策略，最小化用户干扰
 */
@AndroidEntryPoint
class PipeCatForegroundService : Service() {

    @Inject
    lateinit var pipeCatService: PipeCatService

    @Inject
    lateinit var pipeCatConnectionManager: PipeCatConnectionManager

    @Inject
    lateinit var configurationManager: ConfigurationManager

    companion object {
        private const val TAG = "PipeCatForegroundService"
        private const val NOTIFICATION_CHANNEL_ID = "pipecat_service_channel"
        private const val NOTIFICATION_ID = 1001

        // 服务动作
        const val ACTION_START_SERVICE = "start_pipecat_service"
        const val ACTION_STOP_SERVICE = "stop_pipecat_service"
        const val ACTION_RETURN_TO_APP = "return_to_app"
        const val ACTION_ACTIVATE_SESSION = "activate_session"
        const val ACTION_DEACTIVATE_SESSION = "deactivate_session"
    }

    // 服务状态管理
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var notificationJob: Job? = null
    private var isServiceRunning = false
    private var isActiveSession = false
    private var lastNotificationTime = 0L

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "=== 常驻PipeCatForegroundService创建 ===")
        createNotificationChannel()
        initializeGlassesIntegration()
        startServiceAsPersistent()
        Log.i(TAG, "常驻服务初始化完成")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "常驻服务接收到命令: $action")

        when (action) {
            ACTION_START_SERVICE -> {
                Log.d(TAG, "启动常驻服务")
                startServiceAsPersistent()
            }
            ACTION_ACTIVATE_SESSION -> {
                Log.d(TAG, "激活PipeCat会话")
                activatePipeCatSession()
            }
            ACTION_DEACTIVATE_SESSION -> {
                Log.d(TAG, "停用PipeCat会话")
                deactivatePipeCatSession()
            }
            ACTION_STOP_SERVICE -> {
                Log.d(TAG, "停止服务命令")
                stopServiceCompletely()
                return START_NOT_STICKY
            }
            ACTION_RETURN_TO_APP -> {
                Log.d(TAG, "返回应用")
                openMainActivity()
                return START_NOT_STICKY
            }
            else -> {
                // 默认启动常驻服务
                Log.d(TAG, "默认启动常驻服务")
                startServiceAsPersistent()
            }
        }

        // 确保服务为常驻模式
        return START_STICKY // 系统杀死后自动重启
    }

    /**
     * 启动为常驻服务
     */
    private fun startServiceAsPersistent() {
        if (isServiceRunning) {
            Log.d(TAG, "常驻服务已运行，更新通知")
            updatePersistentNotification()
            return
        }

        try {
            isServiceRunning = true
            Log.i(TAG, "启动常驻前台服务")

            // 使用最小化通知启动前台服务
            startForeground(NOTIFICATION_ID, createPersistentNotification())

            // 启动通知更新（始终监控）
            startPersistentNotificationUpdates()

            Log.i(TAG, "常驻服务启动成功")
        } catch (e: Exception) {
            Log.e(TAG, "启动常驻服务失败", e)
            isServiceRunning = false
            stopSelf()
        }
    }

    /**
     * 激活PipeCat会话
     */
    private fun activatePipeCatSession() {
        if (isActiveSession) {
            Log.d(TAG, "PipeCat会话已激活")
            return
        }

        serviceScope.launch {
            try {
                Log.i(TAG, "激活PipeCat会话")
                isActiveSession = true

                val config = configurationManager.getCurrentConfig()
                pipeCatConnectionManager.connect(config)

                updateNotificationForActiveSession()
                Log.i(TAG, "PipeCat会话激活成功")
            } catch (e: Exception) {
                Log.e(TAG, "激活PipeCat会话失败", e)
                isActiveSession = false
            }
        }
    }

    /**
     * 停用PipeCat会话
     */
    private fun deactivatePipeCatSession() {
        if (!isActiveSession) {
            Log.d(TAG, "PipeCat会话未激活")
            return
        }

        serviceScope.launch {
            try {
                Log.i(TAG, "停用PipeCat会话")
                isActiveSession = false

                pipeCatConnectionManager.disconnect()

                updateNotificationForIdleState()
                Log.i(TAG, "PipeCat会话已停用")
            } catch (e: Exception) {
                Log.e(TAG, "停用PipeCat会话失败", e)
            }
        }
    }

    /**
     * 完全停止服务
     */
    private fun stopServiceCompletely() {
        Log.w(TAG, "完全停止常驻服务")

        serviceScope.launch {
            try {
                // 停用会话
                deactivatePipeCatSession()

                // 停止所有监控
                notificationJob?.cancel()

                // 清理glasses集成
                cleanupGlassesIntegration()

                // 停止服务
                isServiceRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()

                Log.i(TAG, "常驻服务已完全停止")
            } catch (e: Exception) {
                Log.e(TAG, "完全停止服务时出错", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Jarvis 常驻语音助手",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持语音助手常驻，提供连续语音交互"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建常驻服务通知（最小化干扰）
     */
    private fun createPersistentNotification(): Notification {
        return createNotification(
            title = "Jarvis 语音助手",
            content = "常驻运行 - 随时准备语音交互",
            isConnected = false,
            isBotReady = false,
            isPersistent = true
        )
    }

    private fun createNotification(
        title: String,
        content: String,
        isConnected: Boolean,
        isBotReady: Boolean,
        isPersistent: Boolean = false
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop service intent
        val stopIntent = Intent(this, PipeCatForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Return to app intent
        val returnIntent = Intent(this, PipeCatForegroundService::class.java).apply {
            action = ACTION_RETURN_TO_APP
        }
        val returnPendingIntent = PendingIntent.getService(
            this, 2, returnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // TODO: Replace with app icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // 根据连接状态添加动作按钮
        if (isConnected && !isPersistent) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "停止会话",
                stopPendingIntent
            )
        }

        if (isBotReady && !isPersistent) {
            builder.addAction(
                android.R.drawable.ic_menu_more,
                "打开应用",
                returnPendingIntent
            )
        }

        // 常驻服务添加控制选项
        if (isPersistent) {
            if (isActiveSession) {
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "停用",
                    stopPendingIntent
                )
            }
        }

        return builder.build()
    }

    private fun updateNotification() {
        val connectionState = pipeCatService.connectionState.value

        val (title, content) = when {
            connectionState.isConnecting -> "Jarvis Voice Assistant" to "Connecting..."
            connectionState.isConnected && connectionState.botReady -> "Jarvis Active" to "Voice assistant ready - Tap to interact"
            connectionState.isConnected -> "Jarvis Connected" to "Initializing assistant..."
            connectionState.errorMessage != null -> "Jarvis Error" to "Connection error: ${connectionState.errorMessage}"
            else -> "Jarvis Voice Assistant" to "Standby - Ready to connect"
        }

        val notification = createNotification(
            title = title,
            content = content,
            isConnected = connectionState.isConnected,
            isBotReady = connectionState.botReady
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startNotificationUpdates() {
        notificationJob?.cancel()

        notificationJob = serviceScope.launch {
            try {
                pipeCatService.connectionState.collect { _ ->
                    updateNotification()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating notifications", e)
            }
        }
    }

    /**
     * 更新常驻服务通知
     */
    private fun updatePersistentNotification() {
        val currentTime = System.currentTimeMillis()
        // 限制通知更新频率，避免过度干扰用户
        if (currentTime - lastNotificationTime < 5000) return
        lastNotificationTime = currentTime

        try {
            val notification = createPersistentNotification()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "更新常驻通知失败", e)
        }
    }

    /**
     * 更新活动会话通知
     */
    private fun updateNotificationForActiveSession() {
        try {
            val notification = createNotification(
                title = "Jarvis 活动会话",
                content = "语音助手运行中 - 随时响应",
                isConnected = true,
                isBotReady = true,
                isPersistent = true
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "更新活动会话通知失败", e)
        }
    }

    /**
     * 更新空闲状态通知
     */
    private fun updateNotificationForIdleState() {
        try {
            val notification = createPersistentNotification()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "更新空闲状态通知失败", e)
        }
    }

    private fun openMainActivity() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
            }
            startActivity(intent)
            Log.d(TAG, "MainActivity opened from notification")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening MainActivity", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PipeCatForegroundService destroyed")

        // Clean up resources
        notificationJob?.cancel()
        cleanupGlassesIntegration()
        serviceScope.cancel()
        isServiceRunning = false
    }

    /**
     * Initialize glasses integration
     */
    private fun initializeGlassesIntegration() {
        try {
            Log.d(TAG, "Initializing glasses integration")
            // Set up CxrApi listener for glasses session state changes
            CxrApi.getInstance().setSceneStatusUpdateListener { sceneStatus ->
                handleGlassesSessionStateChange(sceneStatus.isAiAssistRunning)
            }
            CxrApi.getInstance().setAudioStreamListener(object: AudioStreamListener {
                override fun onStartAudioStream(p0: Int, p1: String?) {
                    // Close audio stream triggered by AI assistant, we use bluetooth sco for voice
                    if ("AI_assistant".equals(p1)) {
                        CxrApi.getInstance().closeAudioRecord("AI_assistant")
                    }
                }

                override fun onAudioStream(p0: ByteArray?, p1: Int, p2: Int) {

                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing glasses integration", e)
        }
    }

    /**
     * 启动常驻通知更新（低频更新）
     */
    private fun startPersistentNotificationUpdates() {
        notificationJob?.cancel()

        notificationJob = serviceScope.launch {
            try {
                // 低频监控连接状态变化
                pipeCatService.connectionState.collect { state ->
                    // 仅在状态真正变化时更新通知
                    if (state.isConnected || state.botReady || state.errorMessage != null) {
                        updateNotificationForActiveSession()
                    } else {
                        updateNotificationForIdleState()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "常驻通知更新出错", e)
            }
        }
    }

    /**
     * Handle glasses session state changes
     */
    private fun handleGlassesSessionStateChange(isAiAssistRunning: Boolean) {
        serviceScope.launch {
            try {
                val connectionState = pipeCatService.connectionState.value
                Log.d(TAG, "Glasses AI assist state changed: isRunning=$isAiAssistRunning, isConnected=${connectionState.isConnected}")

                when {
                    isAiAssistRunning && !connectionState.isConnected -> {
                        Log.d(TAG, "Glasses AI assist started - connecting PipeCat")
                        try {
                            activatePipeCatSession()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error auto-connecting PipeCat for glasses", e)
                        }
                    }
                    !isAiAssistRunning && connectionState.isConnected -> {
                        Log.d(TAG, "Glasses AI assist stopped - disconnecting PipeCat")
                        try {
                            deactivatePipeCatSession()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error auto-disconnecting PipeCat for glasses", e)
                        }
                    }
                    else -> {
                        Log.d(TAG, "No action needed - glasses state consistent with PipeCat connection")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling glasses session state change", e)
            }
        }
    }

    /**
     * Clean up glasses integration
     */
    private fun cleanupGlassesIntegration() {
        try {
            Log.d(TAG, "Cleaning up glasses integration")
            // Clear the CxrApi listener
            CxrApi.getInstance().setSceneStatusUpdateListener(null)
            CxrApi.getInstance().setAudioStreamListener(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up glasses integration", e)
        }
    }
}