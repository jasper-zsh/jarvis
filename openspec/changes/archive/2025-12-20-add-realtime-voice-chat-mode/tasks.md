## 1. Interaction + State
- [ ] 1.1 Design realtime mode entry/exit UX on a separate screen (drawer/tab entry), input controls (mic/video), and playback affordances aligned with overall app style.
- [ ] 1.2 Define session/update payloads for modalities, voice, audio formats, VAD/manual commit, and surface errors/loading states.

## 2. Integration Layer
- [ ] 2.1 Define provider-agnostic realtime interface (connect, session.update, input buffers, response events, lifecycle/cancel) and thread it into chat domain.
- [ ] 2.2 Map audio/video capture outputs to input buffer events (append/commit) including Base64 encoding and VAD/manual submission handling.

## 3. DashScope Omni Realtime
- [ ] 3.1 Implement DashScope adapter using SDK (>=2.20.9), supporting model/url/apikey config and lifecycle callbacks.
- [ ] 3.2 Send session.update with modalities [text,audio], voice (Cherry), pcm16 input/pcm24 output, instructions, and optional server_vad tuning.
- [ ] 3.3 Handle response.text/audio transcript/audio streams, close codes/reasons, and reconnection/cleanup semantics.

## 4. Realtime Config
- [ ] 4.1 Create a dedicated realtime model config repository (separate from chat LLM config) storing provider/model/url/api-key/VAD/voice settings.
- [ ] 4.2 Build UI screens for realtime config management (list/detail/edit) reachable from the realtime page/drawer/tab.
- [ ] 4.3 Wire realtime session creation to consume these configs and validate credentials/regions before connect.

## 5. Validation
- [ ] 5.1 Add configuration docs for API key/env + region URL.
- [ ] 5.2 Add tests or instrumentation to cover event sequencing (connect -> session.update -> input append/commit -> response) and error paths.
- [ ] 5.3 Run openspec validate and relevant build/smoke checks.
