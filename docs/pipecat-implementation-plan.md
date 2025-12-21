# PipeCat Implementation Plan

## Project Structure Overview

This implementation plan outlines all the components that need to be created or modified to integrate PipeCat SDK into the Jarvis Android app.

## Phase 1: Core Infrastructure Setup

### 1.1 Dependencies and Configuration

#### Files to Modify:
- `app/build.gradle.kts` - Add PipeCat dependencies
- `gradle/libs.versions.toml` - Add PipeCat version definitions
- `app/src/main/AndroidManifest.xml` - Add required permissions

#### Implementation Details:
```kotlin
// gradle/libs.versions.toml - Add these entries
pipecatClient = "1.1.0"
kotlinxSerializationJson = "1.7.1"
accompanistPermissions = "0.34.0"

[libraries]
pipecat-client-smallwebrtc = { module = "ai.pipecat:small-webrtc-transport", version.ref = "pipecatClient" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
accompanist-permissions = { module = "com.google.accompanist:accompanist-permissions", version.ref = "accompanistPermissions" }

// app/build.gradle.kts - Add these dependencies
implementation(libs.pipecat.client.smallwebrtc)
implementation(libs.kotlinx.serialization.json)
implementation(libs.accompanist.permissions)
```

### 1.2 Domain Layer Extensions

#### Files to Create:
- `app/src/main/java/pro/sihao/jarvis/domain/model/PipeCatModels.kt`
- `app/src/main/java/pro/sihao/jarvis/domain/service/PipeCatService.kt`
- `app/src/main/java/pro/sihao/jarvis/domain/model/ChatMode.kt`

#### Files to Modify:
- `app/src/main/java/pro/sihao/jarvis/domain/model/Message.kt` - Extend for real-time content

#### Implementation Details:
```kotlin
// PipeCatModels.kt
data class PipeCatConfig(
    val enableMic: Boolean = true,
    val enableCam: Boolean = false,
    val botId: String? = null,
    val baseUrl: String,
    val apiKey: String,
    val customHeaders: Map<String, String> = emptyMap()
)

sealed class PipeCatEvent {
    data class TransportStateChanged(val state: TransportState) : PipeCatEvent()
    data class BotReady(val data: BotReadyData) : PipeCatEvent()
    data class UserTranscript(val text: String, val timestamp: Date = Date()) : PipeCatEvent()
    data class BotResponse(val text: String, val timestamp: Date = Date()) : PipeCatEvent()
    data class BotStartedSpeaking(val timestamp: Date = Date()) : PipeCatEvent()
    data class BotStoppedSpeaking(val timestamp: Date = Date()) : PipeCatEvent()
    data class AudioLevelChanged(val level: Float, val isUser: Boolean) : PipeCatEvent()
    data class Error(val message: String, val throwable: Throwable? = null) : PipeCatEvent()
    object Disconnected : PipeCatEvent()
}

data class PipeCatConnectionState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val botReady: Boolean = false,
    val botIsSpeaking: Boolean = false,
    val userIsSpeaking: Boolean = false,
    val botAudioLevel: Float = 0f,
    val userAudioLevel: Float = 0f,
    val errorMessage: String? = null,
    val config: PipeCatConfig? = null
)

// ChatMode.kt
enum class ChatMode {
    TEXT,
    REALTIME,
    GLASSES
}

// Message.kt - Add these extensions
enum class ContentType {
    TEXT,
    VOICE,
    PHOTO,
    REALTIME_TRANSCRIPT,
    REALTIME_RESPONSE
}

// PipeCatService.kt
interface PipeCatService {
    suspend fun startRealtimeSession(config: PipeCatConfig): Flow<PipeCatEvent>
    suspend fun stopRealtimeSession()
    fun toggleMicrophone(enabled: Boolean)
    fun toggleCamera(enabled: Boolean)
    fun getCurrentState(): PipeCatConnectionState
}
```

## Phase 2: Data Layer Implementation

### 2.1 Service Implementation

#### Files to Create:
- `app/src/main/java/pro/sihao/jarvis/data/service/PipeCatServiceImpl.kt`
- `app/src/main/java/pro/sihao/jarvis/data/webrtc/PipeCatConnectionManager.kt`
- `app/src/main/java/pro/sihao/jarvis/data/webrtc/PipeCatClientWrapper.kt`

#### Implementation Details:
```kotlin
// PipeCatServiceImpl.kt
@Singleton
class PipeCatServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageRepository: MessageRepository
) : PipeCatService {
    
    private val clientWrapper = PipeCatClientWrapper(context)
    private val _connectionState = MutableStateFlow(PipeCatConnectionState())
    override val connectionState: StateFlow<PipeCatConnectionState> = _connectionState
    
    override suspend fun startRealtimeSession(config: PipeCatConfig): Flow<PipeCatEvent> = flow {
        clientWrapper.initialize(config)
        clientWrapper.eventFlow.collect { event ->
            _connectionState.update { state -> 
                when (event) {
                    is PipeCatEvent.TransportStateChanged -> state.copy(isConnected = event.state == TransportState.Connected)
                    is PipeCatEvent.BotReady -> state.copy(botReady = true)
                    is PipeCatEvent.BotStartedSpeaking -> state.copy(botIsSpeaking = true)
                    is PipeCatEvent.BotStoppedSpeaking -> state.copy(botIsSpeaking = false)
                    is PipeCatEvent.AudioLevelChanged -> state.copy(
                        botAudioLevel = if (!event.isUser) event.level else state.botAudioLevel,
                        userAudioLevel = if (event.isUser) event.level else state.userAudioLevel
                    )
                    is PipeCatEvent.Error -> state.copy(errorMessage = event.message)
                    else -> state
                }
            }
            emit(event)
        }
    }
    
    // Implementation details...
}

// PipeCatConnectionManager.kt
@Singleton
class PipeCatConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pipeCatService: PipeCatService,
    private val messageRepository: MessageRepository
) {
    private val _connectionState = MutableStateFlow(PipeCatConnectionState())
    val connectionState: StateFlow<PipeCatConnectionState> = _connectionState
    
    suspend fun connect(config: PipeCatConfig) {
        _connectionState.update { it.copy(isConnecting = true) }
        try {
            pipeCatService.startRealtimeSession(config).collect { event ->
                handlePipeCatEvent(event)
            }
        } catch (e: Exception) {
            _connectionState.update { 
                it.copy(isConnecting = false, errorMessage = e.message) 
            }
        }
    }
    
    private suspend fun handlePipeCatEvent(event: PipeCatEvent) {
        when (event) {
            is PipeCatEvent.UserTranscript -> {
                val message = Message(
                    content = event.text,
                    timestamp = event.timestamp,
                    isFromUser = true,
                    contentType = ContentType.REALTIME_TRANSCRIPT
                )
                messageRepository.insertMessage(message)
            }
            is PipeCatEvent.BotResponse -> {
                val message = Message(
                    content = event.text,
                    timestamp = event.timestamp,
                    isFromUser = false,
                    contentType = ContentType.REALTIME_RESPONSE
                )
                messageRepository.insertMessage(message)
            }
            // Handle other events...
        }
    }
}
```

### 2.2 Repository Extensions

#### Files to Modify:
- `app/src/main/java/pro/sihao/jarvis/data/repository/MessageRepositoryImpl.kt` - Add real-time message handling

#### Implementation Details:
```kotlin
// Add methods to MessageRepository interface and implementation
interface MessageRepository {
    // Existing methods...
    
    suspend fun insertRealtimeMessage(message: Message)
    suspend fun getRealtimeMessages(): Flow<List<Message>>
    suspend fun markRealtimeSessionComplete(sessionId: String)
}
```

## Phase 3: Glasses Integration

### 3.1 Bridge Component

#### Files to Create:
- `app/src/main/java/pro/sihao/jarvis/data/bridge/GlassesPipeCatBridge.kt`
- `app/src/main/java/pro/sihao/jarvis/domain/service/GlassesPipeCatService.kt`

#### Implementation Details:
```kotlin
// GlassesPipeCatBridge.kt
@Singleton
class GlassesPipeCatBridge @Inject constructor(
    private val glassesConnectionManager: GlassesConnectionManager,
    private val pipeCatConnectionManager: PipeCatConnectionManager,
    private val messageRepository: MessageRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    init {
        observeGlassesConnection()
        observePipeCatEvents()
    }
    
    private fun observeGlassesConnection() {
        scope.launch {
            glassesConnectionManager.connectionState.collect { glassesState ->
                when (glassesState.connectionStatus) {
                    GlassesConnectionStatus.CONNECTED -> {
                        // Enable glasses audio input for PipeCat
                        setupGlassesAudioInput()
                    }
                    GlassesConnectionStatus.DISCONNECTED -> {
                        // Fallback to phone microphone
                        pipeCatConnectionManager.toggleMicrophone(true)
                    }
                    else -> { /* Handle other states */ }
                }
            }
        }
    }
    
    private fun setupGlassesAudioInput() {
        // Route glasses audio to PipeCat
        // Handle audio format conversion if needed
    }
    
    private fun observePipeCatEvents() {
        scope.launch {
            pipeCatConnectionManager.connectionState.collect { pipeCatState ->
                // Handle PipeCat responses to glasses
                when {
                    pipeCatState.botIsSpeaking -> {
                        // Send TTS to glasses if enabled
                        sendTtsToGlasses()
                    }
                }
            }
        }
    }
    
    private suspend fun sendTtsToGlasses() {
        // Get latest bot response and send to glasses
        messageRepository.getRealtimeMessages().firstOrNull()
            ?.lastOrNull { !it.isFromUser }
            ?.let { message ->
                if (glassesConnectionManager.connectionState.value.connectionStatus == GlassesConnectionStatus.CONNECTED) {
                    // Send to glasses TTS
                    CxrApi.getInstance().sendTtsContent(message.content)
                }
            }
    }
}
```

## Phase 4: UI Layer Implementation

### 4.1 ViewModel Extensions

#### Files to Create:
- `app/src/main/java/pro/sihao/jarvis/ui/viewmodel/PipeCatViewModel.kt`

#### Files to Modify:
- `app/src/main/java/pro/sihao/jarvis/ui/viewmodel/ChatViewModel.kt` - Add chat mode support

#### Implementation Details:
```kotlin
// PipeCatViewModel.kt
@HiltViewModel
class PipeCatViewModel @Inject constructor(
    private val pipeCatConnectionManager: PipeCatConnectionManager,
    private val glassesPipeCatBridge: GlassesPipeCatBridge
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PipeCatUiState())
    val uiState: StateFlow<PipeCatUiState> = _uiState.asStateFlow()
    
    init {
        observeConnectionState()
    }
    
    private fun observeConnectionState() {
        viewModelScope.launch {
            pipeCatConnectionManager.connectionState.collect { state ->
                _uiState.update { 
                    it.copy(
                        connectionState = state,
                        isConnecting = state.isConnecting,
                        isConnected = state.isConnected
                    )
                }
            }
        }
    }
    
    fun connect(config: PipeCatConfig) {
        viewModelScope.launch {
            pipeCatConnectionManager.connect(config)
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            pipeCatConnectionManager.disconnect()
        }
    }
    
    fun toggleMicrophone(enabled: Boolean) {
        pipeCatConnectionManager.toggleMicrophone(enabled)
    }
    
    fun toggleCamera(enabled: Boolean) {
        pipeCatConnectionManager.toggleCamera(enabled)
    }
}

data class PipeCatUiState(
    val connectionState: PipeCatConnectionState = PipeCatConnectionState(),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: String? = null
)

// ChatViewModel.kt - Add these extensions
data class ChatUiState(
    // Existing fields...
    val chatMode: ChatMode = ChatMode.TEXT,
    val showModeSelector: Boolean = false
)

class ChatViewModel @Inject constructor(
    // Existing dependencies...
    private val pipeCatConnectionManager: PipeCatConnectionManager
) : ViewModel() {
    
    fun setChatMode(mode: ChatMode) {
        _uiState.update { it.copy(chatMode = mode) }
    }
    
    fun toggleModeSelector() {
        _uiState.update { it.copy(showModeSelector = !it.showModeSelector) }
    }
}
```

### 4.2 UI Components

#### Files to Create:
- `app/src/main/java/pro/sihao/jarvis/ui/components/PipeCatControls.kt`
- `app/src/main/java/pro/sihao/jarvis/ui/components/AudioLevelIndicator.kt`
- `app/src/main/java/pro/sihao/jarvis/ui/components/RealtimeStatusIndicator.kt`
- `app/src/main/java/pro/sihao/jarvis/ui/components/ChatModeSelector.kt`

#### Files to Modify:
- `app/src/main/java/pro/sihao/jarvis/ui/screens/ChatScreen.kt` - Add real-time mode support

#### Implementation Details:
```kotlin
// PipeCatControls.kt
@Composable
fun PipeCatControls(
    pipeCatViewModel: PipeCatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by pipeCatViewModel.uiState.collectAsState()
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            onClick = { pipeCatViewModel.toggleMicrophone(!uiState.connectionState.config?.enableMic ?: true) }
        ) {
            Icon(
                imageVector = if (uiState.connectionState.config?.enableMic == true) {
                    Icons.Default.Mic
                } else {
                    Icons.Default.MicOff
                },
                contentDescription = "Toggle Microphone"
            )
        }
        
        IconButton(
            onClick = { pipeCatViewModel.toggleCamera(!uiState.connectionState.config?.enableCam ?: false) }
        ) {
            Icon(
                imageVector = if (uiState.connectionState.config?.enableCam == true) {
                    Icons.Default.VideoCall
                } else {
                    Icons.Default.VideocamOff
                },
                contentDescription = "Toggle Camera"
            )
        }
        
        IconButton(
            onClick = { pipeCatViewModel.disconnect() }
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End Call"
            )
        }
    }
}

// AudioLevelIndicator.kt
@Composable
fun AudioLevelIndicator(
    audioLevel: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = if (isActive) {
                    Color.Green.copy(alpha = audioLevel)
                } else {
                    Color.Gray
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Canvas(modifier = Modifier.size(24.dp)) {
                drawCircle(
                    color = Color.White,
                    radius = audioLevel * 12.dp.toPx()
                )
            }
        }
    }
}

// ChatScreen.kt - Modify to support different modes
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    pipeCatViewModel: PipeCatViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with mode selector
        ChatModeSelector(
            currentMode = uiState.chatMode,
            onModeSelected = viewModel::setChatMode
        )
        
        // Content based on mode
        when (uiState.chatMode) {
            ChatMode.TEXT -> TextChatContent(viewModel, onNavigateToSettings)
            ChatMode.REALTIME -> RealtimeChatContent(viewModel, pipeCatViewModel)
            ChatMode.GLASSES -> GlassesChatContent(viewModel, pipeCatViewModel)
        }
    }
}
```

## Phase 5: Dependency Injection Setup

### 5.1 Hilt Modules

#### Files to Create:
- `app/src/main/java/pro/sihao/jarvis/di/PipeCatModule.kt`

#### Implementation Details:
```kotlin
// PipeCatModule.kt
@Module
@InstallIn(SingletonComponent::class)
object PipeCatModule {
    
    @Provides
    @Singleton
    fun providePipeCatService(
        @ApplicationContext context: Context,
        messageRepository: MessageRepository
    ): PipeCatService {
        return PipeCatServiceImpl(context, messageRepository)
    }
    
    @Provides
    @Singleton
    fun providePipeCatConnectionManager(
        @ApplicationContext context: Context,
        pipeCatService: PipeCatService,
        messageRepository: MessageRepository
    ): PipeCatConnectionManager {
        return PipeCatConnectionManager(context, pipeCatService, messageRepository)
    }
    
    @Provides
    @Singleton
    fun provideGlassesPipeCatBridge(
        glassesConnectionManager: GlassesConnectionManager,
        pipeCatConnectionManager: PipeCatConnectionManager,
        messageRepository: MessageRepository
    ): GlassesPipeCatBridge {
        return GlassesPipeCatBridge(glassesConnectionManager, pipeCatConnectionManager, messageRepository)
    }
}
```

## Phase 6: Configuration and Preferences

### 6.1 Settings Extensions

#### Files to Create:
- `app/src/main/java/pro/sihao/jarvis/data/storage/PipeCatPreferences.kt`

#### Files to Modify:
- `app/src/main/java/pro/sihao/jarvis/ui/screens/SettingsScreen.kt` - Add PipeCat settings

#### Implementation Details:
```kotlin
// PipeCatPreferences.kt
@Singleton
class PipeCatPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("pipecat_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_PIPECAT_BASE_URL = "pipecat_base_url"
        private const val PREF_PIPECAT_API_KEY = "pipecat_api_key"
        private const val PREF_PIPECAT_ENABLE_CAM = "pipecat_enable_cam"
        private const val PREF_PIPECAT_ENABLE_MIC = "pipecat_enable_mic"
        private const val PREF_GLASSES_AUTO_SWITCH = "glasses_auto_switch"
    }
    
    var baseUrl: String
        get() = prefs.getString(PREF_PIPECAT_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(PREF_PIPECAT_BASE_URL, value).apply()
    
    var apiKey: String
        get() = prefs.getString(PREF_PIPECAT_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(PREF_PIPECAT_API_KEY, value).apply()
    
    var enableCameraByDefault: Boolean
        get() = prefs.getBoolean(PREF_PIPECAT_ENABLE_CAM, false)
        set(value) = prefs.edit().putBoolean(PREF_PIPECAT_ENABLE_CAM, value).apply()
    
    var enableMicrophoneByDefault: Boolean
        get() = prefs.getBoolean(PREF_PIPECAT_ENABLE_MIC, true)
        set(value) = prefs.edit().putBoolean(PREF_PIPECAT_ENABLE_MIC, value).apply()
    
    var autoSwitchToGlassesMode: Boolean
        get() = prefs.getBoolean(PREF_GLASSES_AUTO_SWITCH, true)
        set(value) = prefs.edit().putBoolean(PREF_GLASSES_AUTO_SWITCH, value).apply()
}
```

## Implementation Timeline

### Week 1-2: Core Infrastructure
- Set up dependencies and configuration
- Implement domain models and service interfaces
- Create basic PipeCat service implementation

### Week 3-4: Data Layer
- Implement PipeCatConnectionManager
- Create repository extensions
- Add basic UI components

### Week 5-6: Glasses Integration
- Implement GlassesPipeCatBridge
- Integrate glasses audio with PipeCat
- Handle TTS output to glasses

### Week 7-8: UI Enhancement
- Complete UI components
- Implement chat mode switching
- Add settings integration

### Week 9-10: Testing and Polish
- Comprehensive testing
- Error handling improvements
- Performance optimization

## Testing Strategy

### Unit Tests
- PipeCatService implementation
- PipeCatConnectionManager state management
- GlassesPipeCatBridge integration

### Integration Tests
- End-to-end real-time communication
- Glasses audio integration
- UI mode switching

### UI Tests
- Real-time chat interface
- Mode selector functionality
- Settings persistence

## Migration Path

### Phase 1: Parallel Implementation
- Implement PipeCat alongside existing functionality
- Maintain backward compatibility
- Add feature flags for gradual rollout

### Phase 2: Gradual Migration
- Enable PipeCat for beta users
- Collect feedback and iterate
- Optimize performance based on usage

### Phase 3: Full Integration
- Make PipeCat the default real-time solution
- Deprecate old implementation if any
- Complete documentation and user guides