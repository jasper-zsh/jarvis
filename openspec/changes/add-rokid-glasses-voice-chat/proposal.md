# Change: Add Rokid Glasses Voice Chat Bridge

## Why
- Rokid glasses can stream audio in AI assistant mode; we need to capture that stream, detect end-of-speech, and feed it into the existing chat flow.
- After chat responds, we should return the response to the glasses via TTS using the CXR SDK.

## What Changes
- Add AudioStreamListener hookup to the Rokid connection layer to receive glasses microphone audio.
- Integrate a VAD library to detect utterance end and package audio as a voice message into chat mode.
- On completed chat responses, forward text to `CxrApi.getInstance().sendTtsContent(...)` so the glasses hear the reply.

## Impact
- Affected specs: glasses voice integration (new)
- Affected code: glasses connection/service layer, chat/voice pipeline, dependency management (VAD lib)
