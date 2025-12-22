# Change: Move PipeCat connection to foreground service context

## Why
The current PipeCat connection runs in the regular app context, which means when the app loses focus or goes to background, the connection and audio recording are terminated. This breaks the continuous voice interaction experience for users, especially when using glasses where voice input should continue seamlessly.

## What Changes
- **Create a new foreground service** `PipeCatForegroundService` to host all PipeCat connections
- **Refactor PipeCatServiceImpl** to always operate within the foreground service context
- **Move PipeCatConnectionManager** to the foreground service context permanently
- **Add notification management** for foreground service requirements
- **Update AndroidManifest.xml** with proper foreground service permissions
- **Ensure all PipeCat connections start in foreground service regardless of app focus state**

## Impact
- Affected specs: `realtime-voice-chat`
- Affected code:
  - `app/src/main/java/pro/sihao/jarvis/features/realtime/data/service/PipeCatServiceImpl.kt`
  - `app/src/main/java/pro/sihao/jarvis/platform/network/webrtc/PipeCatConnectionManager.kt`
  - `app/src/main/AndroidManifest.xml`
  - New: `app/src/main/java/pro/sihao/jarvis/platform/android/service/PipeCatForegroundService.kt`
- Dependencies: Adds foreground service permission requirements
- User Experience: Enables continuous voice interaction even when app loses focus