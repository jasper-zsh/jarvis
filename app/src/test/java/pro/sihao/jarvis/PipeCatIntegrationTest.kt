package pro.sihao.jarvis

import org.junit.Test
import org.junit.Assert.*
import pro.sihao.jarvis.core.domain.model.PipeCatConnectionState
import pro.sihao.jarvis.core.domain.model.TransportState
import pro.sihao.jarvis.features.realtime.data.service.PipeCatServiceImpl
import android.content.Context
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import pro.sihao.jarvis.platform.android.audio.AudioRoutingManager

/**
 * Simple test to verify PipeCat integration basic functionality
 */
class PipeCatIntegrationTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockAudioRoutingManager: AudioRoutingManager

    private lateinit var pipeCatService: PipeCatServiceImpl

    @Test
    fun `test PipeCat service initialization`() {
        MockitoAnnotations.openMocks(this)

        // Initialize PipeCat service
        pipeCatService = PipeCatServiceImpl(mockContext, mockAudioRoutingManager)
        
        // Verify initial state
        val initialState = pipeCatService.connectionState.value
        assertEquals("Initial connection state should be IDLE",
                   TransportState.IDLE, initialState.transportState)
        assertFalse("Bot should not be connected initially", initialState.isConnected)
        assertFalse("Bot should not be speaking initially", initialState.botIsSpeaking)
    }

    @Test
    fun `test PipeCat connection state flow`() {
        MockitoAnnotations.openMocks(this)

        pipeCatService = PipeCatServiceImpl(mockContext, mockAudioRoutingManager)

        // Test state flow collection
        val states = mutableListOf<PipeCatConnectionState>()

        // Since we can't easily test the actual connection without a real server,
        // we're just verifying that the service can be instantiated and has proper initial state
        val initialState = pipeCatService.connectionState.value
        states.add(initialState)

        // Verify we have at least one state
        assertTrue("Should have at least one state", states.isNotEmpty())

        // Verify initial state properties
        assertEquals("Initial transport state should be IDLE",
                   TransportState.IDLE, states[0].transportState)
        assertFalse("Bot should not be connected initially", states[0].isConnected)
        assertFalse("Bot should not be speaking initially", states[0].botIsSpeaking)
        assertNull("Error message should be null initially", states[0].errorMessage)
    }
}