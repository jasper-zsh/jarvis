## Context
- Streaming responses currently surface through a side-channel listener that mutates UI state while the LLM service emits only a final `Result<String>`.
- MessageBubble treats in-progress assistant responses as opaque `isLoading` placeholders, so users cannot read partial text and cancellation is detached from stream state.
- Chat state stores partial text separately (`streamingContent`), which makes lifecycle (start, cancel, completion) hard to reason about and persists nothing until completion.

## Goals / Non-Goals
- Goals: expose streaming updates through a structured API; render partial assistant text inline with a lightweight in-progress affordance; make cancellation deterministic and reflected in UI; keep media handling unaffected.
- Non-Goals: introduce new providers/models, change persistence schema, or add speculative features like message editing/regen.

## Decisions
- Decision: Replace the ad-hoc partial listener with a structured streaming output (e.g., sealed stream states for chunk/progress/complete/error/canceled) returned by the service so consumers react through a single flow.
- Decision: Represent in-progress assistant messages as explicit UI/domain state (e.g., a transient message entry) that can display accumulating text, while the cancel affordance lives in the input panel (not inside the bubble) and transitions cleanly to the final persisted message.
- Decision: Preserve media bubble handling and timestamps, only extending text rendering for streaming so visual changes stay scoped to assistant responses.

## Risks / Trade-offs
- Changing the LLMService contract will require coordinated updates in ViewModel/UI; partial updates may complicate existing unit tests.
- Without persistence for partial tokens, interruptions (app pause/kill) will still drop in-progress text; acceptable for this refactor but worth noting.

## Open Questions
- Should canceled streams leave a short status entry (e.g., “Response canceled”) or silently disappear?
- Do we need to debounce UI updates for very small tokens to avoid jank on low-end devices?
