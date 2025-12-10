# Change: Clear chat history

## Why
- Users need a quick way to reset a conversation without reinstalling the app or manually deleting data.
- Clearing history also frees local storage tied to messages and attachments.

## What Changes
- Add a user-facing action to clear the current chat history with confirmation.
- Remove persisted messages (and related media) for the selected conversation and reset LLM context.
- Ensure any in-flight requests are cancelled and the chat screen reflects the cleared state.

## Impact
- Affected specs: chat
- Affected code: chat UI, message/message-media storage, LLM context/session handling
