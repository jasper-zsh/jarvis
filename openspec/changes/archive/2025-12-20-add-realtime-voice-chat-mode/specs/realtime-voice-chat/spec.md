## ADDED Requirements
### Requirement: Realtime voice chat mode
The chat experience SHALL provide a dedicated realtime mode on its own screen (accessed via drawer or tab) that enables continuous microphone capture (and optional video frames) with low-latency turn-taking, distinct from batch text/voice messaging.

#### Scenario: User starts realtime voice chat
- **WHEN** the user opens the realtime page and taps start
- **THEN** the UI shows connection/mic state (connecting → listening) and the system opens a realtime session for streaming input/output.

### Requirement: Stream live audio and optional video input
The realtime mode SHALL stream audio buffers (PCM16, Base64) to the backend and MAY stream video frames as images; it SHALL support both server VAD auto-commit and manual commit flows.

#### Scenario: Server VAD auto submission
- **WHEN** server VAD is enabled with configured threshold/silence duration
- **THEN** audio buffers are appended continuously and the backend auto-commits on detected speech end without user action.

#### Scenario: Manual commit submission
- **WHEN** server VAD is disabled or unavailable
- **THEN** the client appends audio/image buffers while recording and explicitly sends a commit event to trigger model response.

### Requirement: Render streamed responses with audio playback
The realtime mode SHALL render streamed transcripts and audio replies, keeping text and audio in sync, and SHALL expose playback controls (play/pause/stop) for the synthesized audio response.

#### Scenario: Mixed text + audio output
- **WHEN** the backend streams response.audio_transcript.* and response.audio.* events
- **THEN** the UI shows incremental transcript text and plays the audio stream, allowing the user to pause/resume or stop playback while retaining the transcript.

### Requirement: Session control and failure feedback
The realtime mode SHALL allow users to cancel/close sessions and SHALL surface backend close codes/reasons or permission/network errors with retry guidance.

#### Scenario: User cancels mid-session
- **WHEN** the user cancels during an active realtime session
- **THEN** audio/video capture stops, the websocket closes cleanly, and the UI shows the cancellation status with an option to restart.
