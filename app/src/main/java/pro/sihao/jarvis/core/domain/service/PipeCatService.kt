package pro.sihao.jarvis.core.domain.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pro.sihao.jarvis.core.domain.model.PipeCatConfig
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.core.domain.model.PipeCatEvent

/**
 * Service interface for PipeCat functionality - handles real-time voice communication
 */
interface PipeCatService {
    /**
     * Current connection state
     */
    val connectionState: StateFlow<PipeCatConnectionState>

    /**
     * Start a real-time session with PipeCat
     * @param config Configuration for the PipeCat session
     * @return Flow of PipeCat events
     */
    suspend fun startRealtimeSession(config: PipeCatConfig): Flow<PipeCatEvent>

    /**
     * Stop the current real-time session
     */
    suspend fun stopRealtimeSession()

    /**
     * Toggle microphone on/off
     * @param enabled Whether microphone should be enabled
     */
    fun toggleMicrophone(enabled: Boolean)

    /**
     * Toggle camera on/off
     * @param enabled Whether camera should be enabled
     */
    fun toggleCamera(enabled: Boolean)
}