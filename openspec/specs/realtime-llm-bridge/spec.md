# realtime-llm-bridge Specification

## Purpose
TBD - created by archiving change add-realtime-voice-chat-mode. Update Purpose after archive.
## Requirements
### Requirement: Realtime model bridge interface
The system SHALL provide a provider-agnostic realtime conversation interface that supports connect/open callbacks, session.update configuration, input buffer append/commit for audio/image, typed response events (text/audio transcript/audio), and close/cancel lifecycle handling.

#### Scenario: Connect and configure session
- **WHEN** chat enters realtime mode
- **THEN** the bridge connects to the selected provider, sends session.update with modalities and audio settings, and exposes event streams for consumption by the interaction layer.

### Requirement: DashScope Omni realtime adapter
The system SHALL implement a DashScope Omni Realtime adapter (SDK >= 2.20.9) using model `qwen3-omni-flash-realtime`, `DASHSCOPE_API_KEY` environment variable, and region-specific websocket URL (Beijing default, Singapore optional override).

#### Scenario: Establish Omni session with config
- **WHEN** the adapter connects
- **THEN** it issues session.update specifying modalities [text,audio], voice Cherry, input_audio_format pcm16, output_audio_format pcm24, instructions per role prompt, and optional server_vad threshold/silence_duration_ms.

### Requirement: DashScope input buffering
The DashScope adapter SHALL accept Base64 PCM16 audio chunks (input_audio_buffer.append) and optional image frames (input_image_buffer.append) and SHALL support manual commit (input_audio_buffer.commit) when VAD is not used.

#### Scenario: Manual commit path
- **WHEN** VAD is disabled and the client finishes sending buffered audio
- **THEN** the adapter sends input_audio_buffer.commit so the backend starts generating a response.

### Requirement: DashScope response handling and closure
The DashScope adapter SHALL surface response.text.delta/done or response.audio_transcript.delta/done plus response.audio.delta/done, and SHALL relay onOpen/onClose (code/reason) to the UI with best-effort cleanup.

#### Scenario: Streamed audio response completes
- **WHEN** response.audio.* events finish and the backend sends response.audio.done
- **THEN** the adapter signals completion to the interaction layer, stops buffering playback, and closes the session on request or when idle.

### Requirement: Realtime model configuration
The system SHALL maintain a dedicated configuration store for realtime LLM models (separate from chat configs) including provider, model name, region URL, API key, VAD defaults, and voice selection, and SHALL expose UI to view/add/edit these settings before starting a realtime session.

#### Scenario: Configure realtime provider
- **WHEN** a user opens the realtime configuration screen via drawer/tab
- **THEN** they can add or edit a realtime provider/model with API key and region URL, and the realtime session uses this configuration when connecting.

