## ADDED Requirements
### Requirement: Glasses Audio To Chat
The system SHALL capture audio from Rokid glasses via `AudioStreamListener`, segment it using voice activity detection, and submit each utterance as a chat voice message.

#### Scenario: Start and segment audio stream
- **WHEN** `AudioStreamListener.onStartAudioStream` fires with an AI assistant command
- **THEN** the system resets audio buffers, begins accepting `onAudioStream` frames, and runs VAD on incoming audio
- **AND** upon detecting end-of-speech (or a timeout), it packages buffered audio as a voice message and delivers it to the chat pipeline.

### Requirement: VAD Library Usage
The system SHALL use a VAD library (WebRTC-based) to detect end-of-speech for glasses audio streams with configurable silence thresholds and a timeout fallback.

#### Scenario: Silence terminates utterance
- **WHEN** the VAD reports sustained silence after speech during an active stream
- **THEN** the buffered audio for that stream is finalized and routed to chat, and the buffer is cleared for the next stream.

### Requirement: Glasses TTS Reply
The system SHALL forward completed chat responses to the connected glasses using `CxrApi.getInstance().sendTtsContent(content)` when the connection is active.

#### Scenario: Send reply to glasses
- **WHEN** a chat response completes after a glasses-originated voice command
- **THEN** the system invokes `sendTtsContent` with the response text so the glasses speak the reply.
