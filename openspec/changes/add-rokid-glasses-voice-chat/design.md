## Context
- Rokid glasses expose microphone audio via `AudioStreamListener` (`onStartAudioStream(codec, cmd)`, `onAudioStream(bytes, offset, length)`).
- We already have a glasses connection manager/service; need to extend it to listen for audio streams and bridge into chat voice input.
- We must choose a VAD library (e.g., WebRTC VAD binding from Maven Central) to detect utterance boundaries on-device.

## Goals / Non-Goals
- Goals: receive audio stream from glasses, run VAD to segment utterances, feed them into chat as voice messages, and send chat replies back to glasses via `sendTtsContent`.
- Non-goals: full speech-to-text on-device (we will reuse existing chat pipeline for transcription/LLM), multi-user routing, or advanced audio buffering beyond what’s needed to detect end-of-speech.

## Decisions
- Use a lightweight WebRTC-based VAD library (to be pulled from Maven Central) to mark end-of-speech; buffer audio frames until VAD signals silence for a configurable window, then submit to chat as a voice message.
- Wire `AudioStreamListener` in the glasses connection service so it’s always available when connected; handle `onStartAudioStream` commands (e.g., `AI_assistant`) to reset buffers and start capturing.
- After the chat pipeline returns a full response, call `CxrApi.getInstance().sendTtsContent(content)` to forward to glasses.

## Risks / Trade-offs
- VAD tuning may be device/room sensitive; need configurable thresholds and a timeout fallback.
- Audio format from CXR needs confirmation (codec/sample rate); may need conversion before VAD or chat upload.
- Streaming volumes could impact memory; must cap buffer size and timeout idle streams.

## Open Questions
- Exact audio encoding/PCM details from `onAudioStream` for correct VAD parameters and upload format.
- Whether multiple audio commands can overlap; if so, do we need queueing? Initial assumption: single active stream.
