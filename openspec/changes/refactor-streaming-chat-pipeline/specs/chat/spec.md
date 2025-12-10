## ADDED Requirements

### Requirement: Streaming Response Display
The chat interface SHALL render streamed assistant responses as readable in-progress bubbles instead of generic loading placeholders.

#### Scenario: Partial response visible
- **WHEN** the assistant response is streaming
- **THEN** the chat shows the accumulated partial text inside an assistant-side bubble
- **AND** the bubble indicates in-progress status without hiding text
- **AND** the partial content updates as new chunks arrive without disrupting scroll position

### Requirement: Streaming Cancellation Feedback
The chat interface SHALL allow canceling an in-progress assistant response and reflect the outcome in the conversation.

#### Scenario: User cancels stream
- **WHEN** the user cancels while a streamed response is in progress via the input panel control (not inside the bubble)
- **THEN** the in-progress bubble stops updating immediately
- **AND** the chat shows that the response ended (canceled or cleared)
- **AND** the user can send a new message right after cancellation
