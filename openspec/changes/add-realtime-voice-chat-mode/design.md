## Context
- Current chat supports batch text/voice messages; no realtime turn-taking or live audio responses.
- We already capture audio (VoiceRecorder/VoicePlayer) and manage media files; we need streaming capture + playback pipeline and a dedicated UI surface (separate page via drawer/tab) to control it without affecting the existing chat screen.
- Backend integration today is HTTP streaming for text; DashScope Omni Realtime requires websocket with session.update, input buffer append/commit, and audio/text response events.

## Goals / Non-Goals
- Goals: introduce a realtime voice chat mode with optional video frames; define stable UI/interaction contract; add a provider-agnostic realtime integration layer with a first DashScope Omni adapter; support text+audio output with low latency and cancel/cleanup behaviors; provide a dedicated config repository/UI for realtime models separate from chat LLM config.
- Non-Goals: full telephony/call UI, multi-party conferencing, offline ASR/LLM, or persistence of partial audio streams beyond session scope.

## Decisions
- Interaction mode: add a dedicated realtime page (reachable via drawer or tab, not a toggle in the current chat view); within that page, show mic state (connecting/listening/sending/responding), optional video capture indicator, and playback controls for streamed audio output. Keep it separate from batch voice messages to avoid mixing flows.
- Configuration: introduce a realtime model configuration repository and UI (own screen/section) to store provider, model, region URL, API key, VAD defaults, and voice options for realtime adapters; do not reuse chat LLM config to avoid cross-protocol leakage.
- Session config: always issue a session.update after connect to declare modalities [text,audio], voice (Cherry), input format pcm16, output format pcm24, instructions (role prompt), and VAD config when enabled; allow manual commit path when VAD is disabled.
- Integration layer: create a realtime conversation interface (connect, send session updates, append audio/image chunks, optional commit, receive typed events, close/cancel). The chat layer depends on this interface, not on DashScope specifics.
- Encoding: capture raw PCM16 audio frames for input; Base64-encode buffers before sending input_audio_buffer.append; video frames (when present) encoded from stills to Base64 and sent via input_image_buffer.append.
- Output handling: render response.audio_transcript.* as live captions and response.audio.* as decoded audio stream for playback; keep response.text.* support for text-only models.
- Lifecycle: require explicit close on session end; propagate onOpen/onEvent/onClose callbacks; surface close code/reason to UI for retry guidance.

## Risks / Trade-offs
- Websocket stability: mobile networks may drop; we need reconnect policies and user-facing retry affordances. Mitigation: expose close codes/reasons and allow re-connect attempts; keep state machine simple.
- Latency vs. battery: continuous audio capture/video frames may drain battery. Mitigation: gate video to optional input; pause capture when app backgrounded; short timeouts for idle sessions.
- SDK compatibility: DashScope SDK version requirements (>=2.20.9) must align with build; mitigation: lock version in catalog and wrap API usage behind adapter to ease upgrades.
- VAD accuracy: server_vad thresholds may mis-detect; mitigation: allow manual commit mode and configurable thresholds.

## Migration Plan
1) Add realtime interaction state and UI affordances guarded behind the new mode toggle.
2) Introduce the provider-agnostic realtime interface and wire chat viewmodel to it.
3) Implement DashScope Omni adapter using SDK websocket flow and session.update contract.
4) Integrate audio/video capture buffers and response playback, then gate behind feature flag if needed.
5) Add docs/config guidance and tests; validate via openspec and build checks.

## Open Questions
- Should we auto-start streaming on mode entry or require explicit “start listening”?
- Do we buffer transcript history in DB or keep realtime-only? (Assumed realtime-only for now.)
- What retry/backoff policy should we use for websocket reconnects?
