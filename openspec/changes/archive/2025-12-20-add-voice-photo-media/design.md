## Context

The current chat implementation only supports text-based messaging. To create a more engaging and natural user experience, we need to extend the chat functionality to handle voice messages and photos. This requires changes to the data model, UI components, and integration with Android's media APIs.

## Goals / Non-Goals

**Goals:**
- Enable users to record and send voice messages with playback functionality
- Allow users to capture or select photos to send in chat
- Maintain backward compatibility with existing text-only conversations
- Provide efficient media storage and compression
- Support speech-to-text transcription for voice messages
- Enable vision analysis of photos by the LLM

**Non-Goals:**
- Video messaging support (out of scope for this change)
- File sharing beyond photos
- Real-time voice calls
- Advanced media editing capabilities

## Decisions

### Message Model Extension
- **Decision**: Extend the existing Message model with `contentType` enum (TEXT, VOICE, PHOTO) and `mediaUrl` field
- **Alternatives considered**: Separate message tables for each media type, base64 encoding in database
- **Rationale**: Unified approach maintains simplicity, supports future media types, external storage prevents database bloat

### Media Storage Strategy
- **Decision**: Store media files in app's internal storage with UUID-based filenames, maintain metadata in database
- **Alternatives considered**: External storage, cache-only storage, database blob storage
- **Rationale**: Internal storage is secure, persists across app sessions, and respects user privacy while maintaining good performance

### Voice Recording Implementation
- **Decision**: Use Android MediaRecorder with AAC compression for voice messages
- **Alternatives considered**: AudioRecord with custom encoding, third-party libraries
- **Rationale**: MediaRecorder provides good balance of quality, file size, and API simplicity

### Photo Integration
- **Decision**: Use CameraX for camera capture and MediaStore API for gallery selection
- **Alternatives considered**: Intent-based camera/gallery, third-party camera libraries
- **Rationale**: CameraX provides consistent experience across Android versions and devices

### UI Component Strategy
- **Decision**: Extend existing MessageInput component with media controls, create new MediaMessageBubble component
- **Alternatives considered**: Separate media input screens, overlay modals
- **Rationale**: Integrated approach maintains conversation flow and reduces friction

## Risks / Trade-offs

- **Storage Space Risk**: Media files will consume device storage
  - **Mitigation**: Implement automatic cleanup of old media, compression limits
- **Performance Risk**: Large photos may impact UI performance
  - **Mitigation**: Generate thumbnails, lazy loading, background processing
- **Privacy Risk**: Access to camera and microphone requires careful permission handling
  - **Mitigation**: Clear permission requests, opt-in behavior, secure storage
- **LLM Integration Complexity**: Vision and voice processing adds API complexity
  - **Mitigation**: Modular design, fallback to text-only when vision unavailable

## Migration Plan

1. **Database Schema Migration**: Extend MessageEntity with new fields for media type and URL
2. **UI Migration**: Gradually extend MessageInput component without breaking existing text functionality
3. **API Migration**: Update LLM service to handle multimodal requests when available
4. **Storage Migration**: Create media directory structure and cleanup procedures
5. **Rollback**: Database migration includes downgrade path, media files can be cleared

## Open Questions

- What should be the maximum duration for voice messages? (suggested: 2 minutes)
- What should be the maximum resolution/size for photos? (suggested: 1920x1080, 5MB)
- Should voice messages be transcribed automatically or on-demand?
- How should we handle users without camera/microphone permissions?
- Should media be deleted after conversation is cleared?