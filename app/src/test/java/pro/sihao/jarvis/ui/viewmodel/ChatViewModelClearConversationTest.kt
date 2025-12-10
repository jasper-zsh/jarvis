package pro.sihao.jarvis.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pro.sihao.jarvis.data.network.NetworkMonitor
import pro.sihao.jarvis.data.repository.ModelConfigRepository
import pro.sihao.jarvis.data.repository.ProviderRepository
import pro.sihao.jarvis.data.storage.MediaStorageManager
import pro.sihao.jarvis.domain.model.ContentType
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.domain.repository.MessageRepository
import pro.sihao.jarvis.domain.service.LLMService
import pro.sihao.jarvis.media.PhotoCaptureManager
import pro.sihao.jarvis.media.VoicePlayer
import pro.sihao.jarvis.media.VoiceRecorder
import pro.sihao.jarvis.permission.PermissionManager
import java.util.Date
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelClearConversationTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var messageRepository: MessageRepository
    @Mock private lateinit var llmService: LLMService
    @Mock private lateinit var networkMonitor: NetworkMonitor
    @Mock private lateinit var providerRepository: ProviderRepository
    @Mock private lateinit var modelConfigRepository: ModelConfigRepository
    @Mock private lateinit var permissionManager: PermissionManager
    @Mock private lateinit var mediaStorageManager: MediaStorageManager
    @Mock private lateinit var voiceRecorder: VoiceRecorder
    @Mock private lateinit var voicePlayer: VoicePlayer
    @Mock private lateinit var photoCaptureManager: PhotoCaptureManager

    private lateinit var viewModel: ChatViewModel

    private val messagesFlow = MutableStateFlow(
        listOf(
            Message(
                id = 1,
                content = "Hi",
                timestamp = Date(),
                isFromUser = true,
                contentType = ContentType.TEXT
            ),
            Message(
                id = 2,
                content = "Photo",
                timestamp = Date(),
                isFromUser = true,
                contentType = ContentType.PHOTO,
                mediaUrl = "/tmp/photo.jpg",
                thumbnailUrl = "/tmp/thumb.jpg"
            )
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockitoRule.apply { }

        // Flows and defaults
        whenever(messageRepository.getAllMessages()).thenReturn(messagesFlow)
        whenever(networkMonitor.isConnected).thenReturn(flowOf(true))
        whenever(providerRepository.getActiveProviders()).thenReturn(flowOf(emptyList()))
        whenever(modelConfigRepository.getActiveModelConfig()).thenReturn(null)
        whenever(modelConfigRepository.getDefaultModelForProvider(any())).thenReturn(null)
        whenever(modelConfigRepository.getFirstActiveModel()).thenReturn(null)
        whenever(modelConfigRepository.getFirstActiveModelForProvider(any())).thenReturn(null)
        whenever(permissionManager.getMediaPermissionsSummary()).thenReturn(
            PermissionManager.MediaPermissionsSummary(
                voiceRecordingStatus = PermissionManager.PermissionStatus.GRANTED,
                cameraStatus = PermissionManager.PermissionStatus.GRANTED,
                galleryStatus = PermissionManager.PermissionStatus.GRANTED,
                hasMicrophoneHardware = true,
                hasCameraHardware = true
            )
        )
        whenever(voiceRecorder.recordingState).thenReturn(MutableStateFlow(VoiceRecorder.RecordingState.IDLE))
        whenever(voiceRecorder.recordingDuration).thenReturn(MutableStateFlow(0L))
        whenever(voicePlayer.playbackState).thenReturn(MutableStateFlow(VoicePlayer.PlaybackState.IDLE))
        whenever(voicePlayer.playbackPosition).thenReturn(MutableStateFlow(0L))
        doNothing().whenever(llmService).setPartialListener(any())

        viewModel = ChatViewModel(
            messageRepository = messageRepository,
            llmService = llmService,
            networkMonitor = networkMonitor,
            providerRepository = providerRepository,
            modelConfigRepository = modelConfigRepository,
            permissionManager = permissionManager,
            mediaStorageManager = mediaStorageManager,
            voiceRecorder = voiceRecorder,
            voicePlayer = voicePlayer,
            photoCaptureManager = photoCaptureManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `clearConversation cancels work, deletes data, and resets state`() = runTest {
        // When
        viewModel.clearConversation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(llmService).cancelActiveRequest()
        verify(voiceRecorder).cancelRecording()
        verify(voicePlayer).stop()
        verify(messageRepository).clearConversation()
        verify(mediaStorageManager).deleteFile("/tmp/photo.jpg")
        verify(mediaStorageManager).deleteFile("/tmp/thumb.jpg")

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertTrue(state.inputMessage.isEmpty())
    }
}
