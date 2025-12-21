## MODIFIED Requirements
### Requirement: Stream live audio and optional video input
The realtime mode SHALL stream audio buffers (PCM16, Base64) to the backend and MAY stream video frames as images; it SHALL support both server VAD auto-commit and manual commit flows, and SHALL automatically route audio through connected Bluetooth devices (including glasses) when available.

#### Scenario: Server VAD auto submission
- **WHEN** server VAD is enabled with configured threshold/silence duration and a Bluetooth device is connected
- **THEN** audio buffers are captured from the Bluetooth device microphone, appended continuously, and the backend auto-commits on detected speech end without user action.

#### Scenario: Manual commit submission with glasses
- **WHEN** server VAD is disabled or unavailable and glasses are connected as Bluetooth audio device
- **THEN** the client appends audio buffers from the glasses microphone while recording and explicitly sends a commit event to trigger model response.

## REMOVED Requirements
### Requirement: Custom audio routing manager
**Reason**: Complex audio routing through AudioRoutingManager creates unnecessary complexity
**Migration**: Use Android's standard AudioManager to automatically route audio through Bluetooth devices

### Requirement: Custom glasses audio processing
**Reason**: Custom audio processing and level calculation should not be handled at application level
**Migration**: Rely on Android's built-in audio processing and standard Bluetooth SCO audio routing