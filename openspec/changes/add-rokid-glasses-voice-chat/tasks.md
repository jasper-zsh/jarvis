## 1. Implementation
- [x] 1.1 Confirm audio format from `AudioStreamListener` (codec/sample rate/endianness) and map to the VAD library input expectations.
- [x] 1.2 Add VAD dependency (Silero VAD) and a small wrapper to feed audio frames and detect end-of-speech with timeout safeguards.
- [x] 1.3 Extend glasses connection manager/service to register an `AudioStreamListener`, buffer incoming frames per stream, and emit completed utterances to the chat pipeline as voice messages.
- [x] 1.4 On completed chat responses, forward text to `CxrApi.getInstance().sendTtsContent(content)` to speak back on the glasses; ensure this only runs when connected.
- [x] 1.5 Add UI/state indicators for active listening/processing (optional minimal badge) and error handling for audio capture or VAD failures.
- [x] 1.6 Validation: manual device test (enter AI assistant mode, speak, see chat message, hear TTS reply); add unit/logic tests around VAD wrapper and buffering timeouts where feasible.
