## ADDED Requirements

### Requirement: Foreground service-based voice sessions
All realtime voice chat sessions SHALL operate within a foreground service context, ensuring continuous audio recording and connection management regardless of app focus state, with proper system notifications for user awareness.

#### Scenario: Session initiation in foreground service
- **WHEN** the user starts a realtime voice session from any app context
- **THEN** the system SHALL start a foreground service to host the PipeCat connection
- **AND** audio recording SHALL be managed through the foreground service
- **AND** a persistent notification SHALL be displayed indicating active voice session
- **AND** the session SHALL operate identically regardless of app focus state

#### Scenario: App lifecycle independence
- **WHEN** the app loses focus or goes to background during an active session
- **THEN** the realtime voice session SHALL continue operating without interruption
- **AND** audio recording SHALL persist through the foreground service
- **AND** the user SHALL continue to receive voice interaction through glasses or connected devices
- **AND** the notification SHALL remain visible to indicate active session

### Requirement: Foreground service notification management
The system SHALL display appropriate notifications when hosting PipeCat connections in foreground service context, with clear user controls for managing the active session and maintaining user awareness.

#### Scenario: Persistent notification display
- **WHEN** a realtime voice session is active
- **THEN** a foreground service notification SHALL continuously display session status (connecting/ready/recording/processing)
- **AND** the notification SHALL include controls to stop the active session
- **AND** tapping the notification SHALL return the user to the realtime voice chat screen
- **AND** the notification SHALL persist even when the app is in background

#### Scenario: Permission and notification handling
- **WHEN** required foreground service permissions are missing or denied
- **THEN** the system SHALL prevent session initiation and inform the user
- **AND** SHALL provide clear guidance for enabling required permissions
- **AND** SHALL not allow voice sessions without proper foreground service authorization

### Requirement: Foreground service lifecycle management
The system SHALL manage foreground service lifecycle automatically, starting and stopping the service based on voice session activity while maintaining proper resource management and cleanup.

#### Scenario: Automatic service management
- **WHEN** a voice session is initiated
- **THEN** the foreground service SHALL be automatically started
- **AND** SHALL acquire necessary resources for audio recording and processing
- **WHEN** the session is ended or stopped
- **THEN** the foreground service SHALL be properly terminated
- **AND** all resources SHALL be cleaned up
- **AND** the notification SHALL be dismissed

#### Scenario: Service failure recovery
- **WHEN** the foreground service encounters unexpected termination or errors
- **THEN** the system SHALL attempt to restart the service and restore session state
- **AND** SHALL provide user feedback about the interruption
- **AND** SHALL offer options to restart the session if recovery fails
- **AND** SHALL ensure proper cleanup of previous session resources

## MODIFIED Requirements

### Requirement: Realtime voice chat mode
The chat experience SHALL provide a dedicated realtime mode on its own screen (accessed via drawer or tab) that enables continuous microphone capture (and optional video frames) with low-latency turn-taking, operating within a foreground service context to ensure consistent behavior regardless of app focus state.

#### Scenario: User starts realtime voice chat
- **WHEN** the user opens the realtime page and taps start
- **THEN** the UI shows connection/mic state (connecting → listening)
- **AND** the system SHALL start a foreground service to host the realtime session
- **AND** SHALL verify and request required foreground service permissions
- **AND** SHALL display a persistent notification indicating active voice session
- **AND** the session SHALL operate through the foreground service regardless of app focus

### Requirement: Stream live audio and optional video input
The realtime mode SHALL stream audio buffers (PCM16, Base64) to the backend through a foreground service context and MAY stream video frames as images; it SHALL support both server VAD auto-commit and manual commit flows with continuous operation.

#### Scenario: Server VAD auto submission
- **WHEN** server VAD is enabled with configured threshold/silence duration
- **THEN** audio buffers SHALL be continuously captured through the foreground service
- **AND** the backend SHALL auto-commit on detected speech end without user action
- **AND** audio capture SHALL continue even if app loses focus during recording

#### Scenario: Manual commit submission
- **WHEN** server VAD is disabled or unavailable
- **THEN** the client SHALL append audio/image buffers through the foreground service while recording
- **AND** SHALL explicitly send a commit event to trigger model response
- **AND** recording SHALL persist through app focus changes

### Requirement: Render streamed responses with audio playback
The realtime mode SHALL render streamed transcripts and audio replies through the foreground service context, keeping text and audio in sync, and SHALL expose playback controls (play/pause/stop) for the synthesized audio response.

#### Scenario: Mixed text + audio output
- **WHEN** the backend streams response.audio_transcript.* and response.audio.* events
- **THEN** the UI SHALL show incremental transcript text and play the audio stream
- **AND** audio playback SHALL be managed through the foreground service context
- **AND** the user SHALL be able to pause/resume or stop playback while retaining the transcript
- **AND** playback SHALL continue even if app goes to background

### Requirement: Session control and failure feedback
The realtime mode SHALL allow users to cancel/close sessions and SHALL surface backend close codes/reasons or permission/network errors with retry guidance, including proper foreground service lifecycle management.

#### Scenario: User cancels mid-session
- **WHEN** the user cancels during an active realtime session
- **THEN** audio/video capture stops, the websocket closes cleanly
- **AND** the foreground service SHALL be properly terminated
- **AND** all notifications SHALL be dismissed
- **AND** the UI SHALL show the cancellation status with an option to restart

#### Scenario: Foreground service specific errors
- **WHEN** foreground service permissions are revoked or system limitations prevent operation
- **THEN** the user SHALL receive clear error messaging about the service limitations
- **AND** the system SHALL offer options to resolve permission issues
- **AND** SHALL prevent session initiation until foreground service requirements are met
- **AND** SHALL provide guidance for enabling necessary system permissions