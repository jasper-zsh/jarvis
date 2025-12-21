# PipeCat Integration Design Document

## Overview

This document outlines the design for integrating PipeCat SDK into the Jarvis Android app to enable real-time voice and video communication capabilities with AI agents.

## Current Architecture Analysis

### Jarvis App Structure
- **UI Layer**: Jetpack Compose with ViewModels
- **Domain Layer**: Clean architecture with services and repositories
- **Data Layer**: Room database, network services, and storage managers
- **DI**: Hilt for dependency injection
- **Existing Glasses Integration**: Bluetooth-based Rokid glasses connection

### Key Components
- `GlassesConnectionManager`: Handles Bluetooth connection to Rokid glasses
- `ChatViewModel`: Manages chat UI state and interactions
- `LLMService`: Handles text-based AI interactions
- `Message`: Domain model for chat messages with support for text, voice, and photo content

## PipeCat SDK Analysis

### Core Components
- `PipecatClientSmallWebRTC`: Main client class for WebRTC transport
- `VoiceClientManager`: Manages client state and callbacks
- `SmallWebRTCTransport`: WebRTC transport implementation
- `PipecatEventCallbacks`: Event handling interface

### Key Features
- Real-time audio/video streaming
- Voice activity detection
- Bot state management
- Transport state management
- Audio level monitoring

## Integration Design

### 1. Architecture Integration Strategy

#### 1.1 Service Layer Extension
Create a new `PipeCatService` that extends the existing `LLMService` interface to provide real-time capabilities:

```kotlin
interface PipeCatService : LLMService {
    suspend fun startRealtimeSession(
        baseUrl: String,
        apiKey: String,
        config: PipeCatConfig
    ): Flow<PipeCatEvent>
    
    suspend fun stopRealtimeSession()
    fun toggleMicrophone(enabled: Boolean)
    fun toggleCamera(enabled: Boolean)
}
```

#### 1.2 Domain Model Extensions
Extend existing models to support PipeCat-specific data:

```kotlin
data class PipeCatConfig(
    val enableMic: Boolean = true,
    val enableCam: Boolean = false,
    val botId: String? = null,
    val customHeaders: Map<String, String> = emptyMap()
)

sealed class PipeCatEvent {
    data class TransportStateChanged(val state: TransportState) : PipeCatEvent()
    data class BotReady(val data: BotReadyData) : PipeCatEvent()
    data class UserTranscript(val text: String) : PipeCatEvent()
    data class BotResponse(val text: String) : PipeCatEvent()
    data class BotStartedSpeaking : PipeCatEvent()
    data class BotStoppedSpeaking : PipeCatEvent()
    data class Error(val message: String) : PipeCatEvent()
}
```

### 2. Component Design

#### 2.1 PipeCatConnectionManager
Create a dedicated manager for PipeCat connections, similar to `GlassesConnectionManager`:

```kotlin
@Singleton
class PipeCatConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pipeCatService: PipeCatService,
    private val messageRepository: MessageRepository
) {
    private val client = mutableStateOf<PipecatClientSmallWebRTC?>(null)
    private val _connectionState = MutableStateFlow(PipeCatConnectionState())
    val connectionState: StateFlow<PipeCatConnectionState> = _connectionState
    
    suspend fun connect(config: PipeCatConfig)
    suspend fun disconnect()
    fun toggleMicrophone(enabled: Boolean)
    fun toggleCamera(enabled: Boolean)
}
```

#### 2.2 PipeCat Glasses Integration
Integrate PipeCat with existing glasses functionality:

```kotlin
class GlassesPipeCatBridge @Inject constructor(
    private val glassesConnectionManager: GlassesConnectionManager,
    private val pipeCatConnectionManager: PipeCatConnectionManager
) {
    // Bridge glasses audio input to PipeCat
    // Handle glasses TTS output from PipeCat responses
    // Manage state synchronization
}
```

### 3. UI Integration Strategy

#### 3.1 Chat Screen Enhancement
Extend `ChatScreen` to support real-time mode:

```kotlin
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    pipeCatViewModel: PipeCatViewModel
) {
    when (viewModel.uiState.value.chatMode) {
        ChatMode.TEXT -> TextChatInterface(viewModel)
        ChatMode.REALTIME -> RealtimeChatInterface(viewModel, pipeCatViewModel)
        ChatMode.GLASSES -> GlassesChatInterface(viewModel, pipeCatViewModel)
    }
}
```

#### 3.2 Real-time UI Components
Create new Compose components for real-time interaction:

```kotlin
@Composable
fun RealtimeChatInterface(
    chatViewModel: ChatViewModel,
    pipeCatViewModel: PipeCatViewModel
) {
    Column {
        RealtimeStatusIndicator(pipeCatViewModel.connectionState)
        AudioLevelIndicator(pipeCatViewModel.audioLevels)
        RealtimeMessagesList(messages)
        RealtimeControls(pipeCatViewModel)
    }
}
```

### 4. Data Flow Design

#### 4.1 Real-time Message Flow
```
User Voice Input → Glasses → PipeCat → AI Agent → PipeCat → TTS → Glasses
                                     ↓
                                 Transcript → Message Repository → UI
```

#### 4.2 State Management
```
GlassesConnectionState ←→ GlassesPipeCatBridge ←→ PipeCatConnectionState
                                     ↓
                              ChatViewModel ←→ UI State
```

## Implementation Plan

### Phase 1: Core PipeCat Integration
1. Add PipeCat dependencies
2. Create `PipeCatService` implementation
3. Implement `PipeCatConnectionManager`
4. Add basic UI components

### Phase 2: Glasses Integration
1. Implement `GlassesPipeCatBridge`
2. Integrate glasses audio with PipeCat
3. Handle TTS output to glasses
4. Synchronize connection states

### Phase 3: UI Enhancement
1. Extend ChatScreen with real-time mode
2. Implement real-time UI components
3. Add mode switching functionality
4. Integrate with existing message system

### Phase 4: Advanced Features
1. Video support integration
2. Advanced audio processing
3. Error handling and recovery
4. Performance optimization

## Dependencies Required

### Build Configuration
```kotlin
// app/build.gradle.kts
dependencies {
    implementation("ai.pipecat:small-webrtc-transport:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}
```

### Permissions
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Migration Strategy

### Backward Compatibility
1. Maintain existing text-based chat functionality
2. Add real-time mode as optional feature
3. Gradual migration of users to enhanced features

### Configuration Management
1. Extend existing preferences system
2. Add PipeCat-specific settings
3. Integrate with existing provider configuration

## Security Considerations

### API Key Management
1. Use existing encrypted storage for API keys
2. Extend provider management for PipeCat services
3. Implement secure token refresh mechanisms

### Network Security
1. Validate server certificates
2. Implement secure WebRTC connections
3. Handle network failures gracefully

## Performance Considerations

### Resource Management
1. Efficient audio/video stream handling
2. Proper lifecycle management
3. Background processing optimization

### Memory Management
1. Stream processing without buffering entire sessions
2. Efficient state management
3. Garbage collection optimization

## Testing Strategy

### Unit Testing
1. Service layer testing
2. State management testing
3. Integration bridge testing

### Integration Testing
1. End-to-end real-time communication
2. Glasses integration testing
3. Error scenario testing

### UI Testing
1. Real-time UI component testing
2. Mode switching testing
3. User interaction testing