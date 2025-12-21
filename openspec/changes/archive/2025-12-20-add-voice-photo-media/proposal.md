# Change: Add Voice and Photo Media Support to Chat

## Why
Enhance the Jarvis chat experience to support multimodal interactions, allowing users to send voice messages and photos for more natural and expressive conversations with the AI assistant.

## What Changes
- Add voice recording and playback capabilities to the chat interface
- Add photo capture and selection from device gallery
- Extend Message model to support different content types (text, voice, photo)
- Implement media storage and compression for efficient handling
- Add UI components for media input and display
- Integrate speech-to-text for voice message transcription
- Add vision capabilities for photo analysis by LLM
- Update message bubble component to handle different media types

## Impact
- Affected specs: chat (extending existing capability)
- Affected code: Message model, ChatViewModel, MessageInput, MessageBubble, chat UI screens
- New dependencies: Android MediaRecorder, CameraX/Gallery, Speech Recognition, Vision API
- Permissions: RECORD_AUDIO, CAMERA, READ_EXTERNAL_STORAGE required
- Storage: Local media cache and database schema changes for media metadata