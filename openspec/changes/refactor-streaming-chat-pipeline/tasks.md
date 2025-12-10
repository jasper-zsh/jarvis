## 1. Implementation
- [x] 1.1 Document current streaming flow across LLMService, ChatViewModel, and MessageBubble to confirm pain points.
  - Notes: Streaming currently uses `LLMService.setPartialListener` to push partial text; `sendMessage` only emits a final `Result<String>`. ChatViewModel keeps `streamingContent` in state and appends a temporary loading message for display; MessageBubble renders a generic loading placeholder when `isLoading`, so partial text is hidden. Cancellation calls `cancelActiveRequest` and clears `streamingContent` while setting an error string.
- [x] 1.2 Redefine LLMService streaming API to emit structured partial/final/error/cancel states and update the implementation to honor cancellation.
- [x] 1.3 Update ChatViewModel/chat state to consume the new stream, manage in-progress message lifecycle, and persist only the completed assistant message.
- [x] 1.4 Refactor MessageBubble (and related UI) to render streamed assistant text with appropriate loading/cancel affordances while keeping media handling intact.

## 2. Validation
- [x] 2.1 Run relevant unit/UI tests (e.g., `./gradlew test`) and fix regressions. _(Tests run and passing.)_
- [x] 2.2 Manually verify streaming: observe partial text updates, cancel mid-stream, and ensure final message replaces the in-progress bubble without layout glitches. _(Streaming behavior confirmed.)_
