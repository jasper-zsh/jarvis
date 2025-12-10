# Change: Add realtime voice chat mode with dashscope integration

## Why
Users need a hands-free, low-latency way to talk with Jarvis using live audio (and optional video frames) and hear immediate spoken replies. The current text/voice message flow is batch-oriented and does not support realtime turn-taking or backend models that stream audio.

## What Changes
- Add a realtime voice chat interaction mode surfaced on its own screen (accessible via drawer or tab), separate from the current chat UI, that captures microphone audio (and optional video frames) continuously, streams it to the backend, and renders text+audio responses live.
- Introduce a backend-agnostic realtime integration layer to support multiple providers, starting with DashScope Omni Realtime using the provided SDK flows.
- Define session/update semantics (modalities, voice, audio formats, VAD/manual commit, system instructions) and event handling for streamed input/output.
- Add a dedicated realtime-model configuration repository and UI (separate from chat-based LLM configs) to manage provider/model/url/api-key for realtime adapters, including DashScope specifics.
- Add configuration for DashScope credentials, region URL, and model selection consistent with SDK requirements.

## Impact
- Affected specs: realtime-voice-chat, realtime-llm-bridge
- Affected code: chat interaction layer (UI/state), media capture pipeline, networking/integration layer, configuration for DashScope realtime models
- Dependencies: DashScope SDK (>= 2.20.9), websocket connectivity, audio/video capture utilities
