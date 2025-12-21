# PipeCat Integration - Components Summary

## Overview

This document provides a comprehensive summary of all components that need to be created or modified to integrate PipeCat SDK into the Jarvis Android app.

## Files to Create

### Domain Layer

#### 1. `app/src/main/java/pro/sihao/jarvis/domain/model/PipeCatModels.kt`
**Purpose**: Define PipeCat-specific domain models
**Key Components**:
- `PipeCatConfig` - Configuration for PipeCat sessions
- `PipeCatEvent` - Sealed class for all PipeCat events
- `PipeCatConnectionState` - State management for PipeCat connections

#### 2. `app/src/main/java/pro/sihao/jarvis/domain/model/ChatMode.kt`
**Purpose**: Define chat modes for the application
**Key Components**:
- `ChatMode` enum (TEXT, REALTIME, GLASSES)

#### 3. `app/src/main/java/pro/sihao/jarvis/domain/service/PipeCatService.kt`
**Purpose**: Service interface for PipeCat functionality
**Key Components**:
- `PipeCatService` interface extending `LLMService`
- Methods for real-time session management

### Data Layer

#### 4. `app/src/main/java/pro/sihao/jarvis/data/service/PipeCatServiceImpl.kt`
**Purpose**: Implementation of PipeCatService
**Key Components**:
- `PipeCatServiceImpl` class
- Integration with PipeCat SDK
- State management and event handling

#### 5. `app/src/main/java/pro/sihao/jarvis/data/webrtc/PipeCatConnectionManager.kt`
**Purpose**: Manage PipeCat connections and state
**Key Components**:
- `PipeCatConnectionManager` class
- Connection lifecycle management
- Event processing and message handling

#### 6. `app/src/main/java/pro/sihao/jarvis/data/webrtc/PipeCatClientWrapper.kt`
**Purpose**: Wrapper around PipeCat SDK client
**Key Components**:
- `PipeCatClientWrapper` class
- Direct SDK integration
- Event stream management

#### 7. `app/src/main/java/pro/sihao/jarvis/data/bridge/GlassesPipeCatBridge.kt`
**Purpose**: Bridge between glasses and PipeCat functionality
**Key Components**:
- `GlassesPipeCatBridge` class
- Audio routing between glasses and PipeCat
- TTS integration with glasses

#### 8. `app/src/main/java/pro/sihao/jarvis/domain/service/GlassesPipeCatService.kt`
**Purpose**: Service interface for glasses-PipeCat integration
**Key Components**:
- `GlassesPipeCatService` interface
- Methods for glasses-specific PipeCat operations

#### 9. `app/src/main/java/pro/sihao/jarvis/data/storage/PipeCatPreferences.kt`
**Purpose**: Manage PipeCat-specific preferences
**Key Components**:
- `PipeCatPreferences` class
- Configuration persistence
- Settings management

### UI Layer

#### 10. `app/src/main/java/pro/sihao/jarvis/ui/viewmodel/PipeCatViewModel.kt`
**Purpose**: ViewModel for PipeCat UI components
**Key Components**:
- `PipeCatViewModel` class
- `PipeCatUiState` data class
- UI state management for real-time features

#### 11. `app/src/main/java/pro/sihao/jarvis/ui/components/PipeCatControls.kt`
**Purpose**: UI controls for PipeCat functionality
**Key Components**:
- `PipeCatControls` Composable
- Microphone and camera toggle buttons
- Connection controls

#### 12. `app/src/main/java/pro/sihao/jarvis/ui/components/AudioLevelIndicator.kt`
**Purpose**: Visual indicator for audio levels
**Key Components**:
- `AudioLevelIndicator` Composable
- Real-time audio level visualization
- User and bot audio indicators

#### 13. `app/src/main/java/pro/sihao/jarvis/ui/components/RealtimeStatusIndicator.kt`
**Purpose**: Status indicator for real-time connections
**Key Components**:
- `RealtimeStatusIndicator` Composable
- Connection state visualization
- Bot status display

#### 14. `app/src/main/java/pro/sihao/jarvis/ui/components/ChatModeSelector.kt`
**Purpose**: UI component for selecting chat modes
**Key Components**:
- `ChatModeSelector` Composable
- Mode switching interface
- Visual mode indicators

### Dependency Injection

#### 15. `app/src/main/java/pro/sihao/jarvis/di/PipeCatModule.kt`
**Purpose**: Hilt module for PipeCat dependencies
**Key Components**:
- `PipeCatModule` object
- Dependency provider methods
- Singleton configuration

## Files to Modify

### Configuration

#### 16. `app/build.gradle.kts`
**Changes**: Add PipeCat dependencies
```kotlin
dependencies {
    implementation(libs.pipecat.client.smallwebrtc)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.accompanist.permissions)
}
```

#### 17. `gradle/libs.versions.toml`
**Changes**: Add PipeCat version definitions
```toml
pipecatClient = "1.1.0"
kotlinxSerializationJson = "1.7.1"
accompanistPermissions = "0.34.0"

[libraries]
pipecat-client-smallwebrtc = { module = "ai.pipecat:small-webrtc-transport", version.ref = "pipecatClient" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
accompanist-permissions = { module = "com.google.accompanist:accompanist-permissions", version.ref = "accompanistPermissions" }
```

#### 18. `app/src/main/AndroidManifest.xml`
**Changes**: Add required permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Domain Layer

#### 19. `app/src/main/java/pro/sihao/jarvis/domain/model/Message.kt`
**Changes**: Extend ContentType enum for real-time content
```kotlin
enum class ContentType {
    TEXT,
    VOICE,
    PHOTO,
    REALTIME_TRANSCRIPT,
    REALTIME_RESPONSE
}
```

#### 20. `app/src/main/java/pro/sihao/jarvis/domain/repository/MessageRepository.kt`
**Changes**: Add real-time message handling methods
```kotlin
interface MessageRepository {
    // Existing methods...
    
    suspend fun insertRealtimeMessage(message: Message)
    suspend fun getRealtimeMessages(): Flow<List<Message>>
    suspend fun markRealtimeSessionComplete(sessionId: String)
}
```

### Data Layer

#### 21. `app/src/main/java/pro/sihao/jarvis/data/repository/MessageRepositoryImpl.kt`
**Changes**: Implement real-time message methods
- Add implementation for new interface methods
- Handle real-time message persistence
- Manage session lifecycle

#### 22. `app/src/main/java/pro/sihao/jarvis/data/network/LLMServiceImpl.kt`
**Changes**: Extend to support PipeCat events
- Add PipeCat event handling
- Integrate with real-time message flow
- Maintain backward compatibility

### UI Layer

#### 23. `app/src/main/java/pro/sihao/jarvis/ui/viewmodel/ChatViewModel.kt`
**Changes**: Add chat mode support
```kotlin
data class ChatUiState(
    // Existing fields...
    val chatMode: ChatMode = ChatMode.TEXT,
    val showModeSelector: Boolean = false
)

class ChatViewModel {
    fun setChatMode(mode: ChatMode)
    fun toggleModeSelector()
}
```

#### 24. `app/src/main/java/pro/sihao/jarvis/ui/screens/ChatScreen.kt`
**Changes**: Add real-time mode support
- Add mode selector component
- Implement different UI based on chat mode
- Integrate PipeCat components

#### 25. `app/src/main/java/pro/sihao/jarvis/ui/screens/SettingsScreen.kt`
**Changes**: Add PipeCat settings
- Add PipeCat configuration UI
- Integrate with PipeCatPreferences
- Add glasses integration settings

## Component Relationships

### Architecture Flow
```
UI Layer (ViewModels, Composables)
    ↓
Domain Layer (Services, Models)
    ↓
Data Layer (Implementations, Repositories)
    ↓
Bridge Layer (GlassesPipeCatBridge)
    ↓
External SDKs (PipeCat, Rokid)
```

### Data Flow
```
User Input → UI Component → ViewModel → Service → PipeCat SDK → AI Agent
    ↓                                                                    ↓
UI Update ← State Flow ← Event Processing ← Bridge Component ← Glasses Audio
```

### Dependency Injection Graph
```
PipeCatModule
    ↓
PipeCatService → PipeCatConnectionManager → PipeCatClientWrapper
    ↓
GlassesPipeCatBridge → GlassesConnectionManager
    ↓
PipeCatViewModel → UI Components
```

## Integration Points

### 1. Existing LLMService Integration
- `PipeCatService` extends `LLMService`
- Maintains existing text-based chat functionality
- Adds real-time capabilities as extension

### 2. Glasses Integration
- `GlassesPipeCatBridge` connects existing `GlassesConnectionManager`
- Routes glasses audio to PipeCat
- Handles TTS output to glasses

### 3. Message System Integration
- Extends existing `Message` model for real-time content
- Integrates with existing `MessageRepository`
- Maintains conversation history across modes

### 4. UI Integration
- Extends existing `ChatScreen` with mode switching
- Reuses existing message components
- Adds new real-time specific components

## Migration Strategy

### Phase 1: Foundation (Week 1-2)
- Create all new domain models and service interfaces
- Set up basic PipeCat service implementation
- Configure dependencies and permissions

### Phase 2: Core Implementation (Week 3-4)
- Implement PipeCatConnectionManager
- Create basic UI components
- Integrate with existing message system

### Phase 3: Glasses Integration (Week 5-6)
- Implement GlassesPipeCatBridge
- Connect glasses audio to PipeCat
- Handle TTS output to glasses

### Phase 4: UI Enhancement (Week 7-8)
- Complete UI components
- Implement chat mode switching
- Add settings integration

### Phase 5: Testing and Polish (Week 9-10)
- Comprehensive testing
- Error handling improvements
- Performance optimization

## Risk Mitigation

### Technical Risks
1. **SDK Compatibility**: Ensure PipeCat SDK version compatibility with Android target SDK
2. **Performance**: Monitor memory usage and CPU impact
3. **Audio Latency**: Optimize audio processing pipeline

### Integration Risks
1. **State Synchronization**: Ensure proper state management between components
2. **Error Handling**: Implement robust error recovery mechanisms
3. **User Experience**: Maintain smooth transition between chat modes

### Mitigation Strategies
1. **Incremental Development**: Implement features incrementally with testing at each step
2. **Feature Flags**: Use feature flags for gradual rollout
3. **Fallback Mechanisms**: Provide fallback to existing functionality when PipeCat fails

## Success Metrics

### Technical Metrics
- Connection establishment time < 3 seconds
- Audio latency < 200ms
- Memory usage increase < 50MB
- Crash rate < 0.1%

### User Experience Metrics
- Mode switching time < 1 second
- Voice recognition accuracy > 90%
- User satisfaction score > 4.0/5.0
- Feature adoption rate > 60%

## Conclusion

This comprehensive component summary provides a clear roadmap for integrating PipeCat SDK into the Jarvis Android app. The implementation maintains backward compatibility while adding powerful real-time capabilities, particularly for glasses integration. The modular design ensures maintainability and extensibility for future enhancements.