package pro.sihao.jarvis.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import pro.sihao.jarvis.connection.GlassesConnectionManager

@AndroidEntryPoint
class GlassesConnectionService : Service() {

    @Inject lateinit var manager: GlassesConnectionManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep service running; manager handles auto-reconnect using application context.
        manager.ensureAutoReconnect()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
