## MODIFIED Requirements

### Requirement: Message Content Support
The system SHALL support multiple content types for chat messages including text, voice recordings, and photos.

#### Scenario: User sends text message
- **WHEN** user types text in the input field and clicks send
- **THEN** the message is created with TEXT content type and delivered normally

#### Scenario: User records and sends voice message
- **WHEN** user holds the voice record button, speaks, and releases
- **THEN** the voice is recorded, compressed, saved locally, and message created with VOICE content type
- **AND** the message displays with playback controls

#### Scenario: User captures and sends photo
- **WHEN** user clicks camera button, takes photo, and confirms send
- **THEN** the photo is captured, compressed, saved locally, and message created with PHOTO content type
- **AND** the message displays with thumbnail and full-screen viewing capability

## ADDED Requirements

### Requirement: Voice Recording Interface
The system SHALL provide voice recording functionality with visual feedback and playback controls.

#### Scenario: Voice recording starts
- **WHEN** user presses and holds the voice record button
- **THEN** recording starts with visual feedback (pulsing animation, timer display)
- **AND** microphone permission is requested if not already granted

#### Scenario: Voice recording completes
- **WHEN** user releases the voice record button
- **THEN** recording stops, audio is compressed and saved
- **AND** voice message bubble appears with duration and playback controls
- **AND** optional speech-to-text transcription is performed in background

#### Scenario: Voice message playback
- **WHEN** user taps play button on voice message
- **THEN** audio playback starts with play/pause toggle and progress indicator

#### Scenario: Voice recording cancelled
- **WHEN** user slides finger away from record button while recording
- **THEN** recording is cancelled and no message is created

### Requirement: Photo Capture and Selection
The system SHALL provide photo capture from camera and selection from device gallery.

#### Scenario: Photo capture from camera
- **WHEN** user taps camera button and selects "Take Photo"
- **THEN** camera interface opens with preview
- **AND** user can capture, review, and confirm photo
- **AND** photo is compressed to optimal size and saved locally

#### Scenario: Photo selection from gallery
- **WHEN** user taps camera button and selects "Choose from Gallery"
- **THEN** gallery picker opens
- **AND** user can browse and select existing photos
- **AND** selected photo is copied to app storage and optimized

#### Scenario: Photo message display
- **WHEN** photo message is displayed in chat
- **THEN** thumbnail image is shown in message bubble
- **AND** tapping thumbnail opens full-screen image viewer
- **AND** image dimensions and file size information is available

### Requirement: Media Storage Management
The system SHALL efficiently store and manage media files with automatic cleanup.

#### Scenario: Media file storage
- **WHEN** voice or photo message is created
- **THEN** media file is saved with UUID filename in internal app storage
- **AND** media metadata (type, size, duration, thumbnail) is stored in database

#### Scenario: Conversation cleanup
- **WHEN** user clears conversation history
- **THEN** associated media files are deleted from device storage
- **AND** database records are removed

#### Scenario: Storage optimization
- **WHEN** storage space becomes limited
- **THEN** oldest media files are automatically removed
- **AND** user is notified when media cleanup occurs

### Requirement: Multimedia LLM Integration
The system SHALL extend LLM integration to process voice and photo content for richer responses.

#### Scenario: Voice message processing by LLM
- **WHEN** voice message is sent to LLM
- **THEN** speech-to-text transcription is generated
- **AND** transcribed text is sent to LLM for response
- **AND** both original audio and transcription are preserved

#### Scenario: Photo analysis by LLM
- **WHEN** photo message is sent to LLM
- **THEN** image is encoded and sent to vision-capable LLM
- **AND** LLM response includes description or analysis of photo content
- **AND** fallback to text-only response if vision unavailable

### Requirement: Enhanced Message Input Controls
The system SHALL provide intuitive controls for switching between text, voice, and photo input modes.

#### Scenario: Input mode switching
- **WHEN** user is in chat input area
- **THEN** text input field is visible by default
- **AND** voice record button and camera button are available
- **AND** mode switches smoothly based on user action

#### Scenario: Media input validation
- **WHEN** user attempts to send media
- **THEN** system validates required permissions (camera, microphone)
- **AND** clear error messages are shown if permissions denied
- **AND** option to open settings is provided

### Requirement: Permission Management
The system SHALL properly handle Android permissions for camera and microphone access.

#### Scenario: First-time permission request
- **WHEN** user first attempts to use camera or microphone
- **THEN** system permission dialog is displayed with clear explanation
- **AND** app gracefully handles permission denial

#### Scenario: Permission denied
- **WHEN** user denies camera or microphone permission
- **THEN** relevant input controls are disabled
- **AND** helpful message explains why permission is needed
- **AND** option to open app settings is provided