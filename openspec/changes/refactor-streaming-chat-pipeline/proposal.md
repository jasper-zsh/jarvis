# Change: Refactor streaming message display and LLM service

## Why
- Streaming assistant responses render as generic loading placeholders, so users cannot see partial text while tokens arrive.
- The LLM service uses a side-channel partial listener, making streaming updates brittle, hard to cancel, and misaligned with chat UI state.

## What Changes
- Refactor message bubbles to render streamed assistant text inline with an in-progress affordance and cancel handling.
- Rework the LLM streaming contract to emit structured partial/final states without out-of-band listeners, keeping cancellation and errors coherent.
- Align chat state management with the new stream model so final messages persist cleanly after streaming ends.

## Impact
- Affected specs: chat, ai-service
- Affected code: MessageBubble/MediaMessageBubble, LLMService/LLMServiceImpl, ChatViewModel/ChatScreen, related message/model wiring
