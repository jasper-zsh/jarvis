## ADDED Requirements

### Requirement: Streaming Response Delivery
The LLM service SHALL provide structured streaming updates for chat completions rather than only emitting a final result.

#### Scenario: Provider streams tokens
- **WHEN** the provider returns a streaming response
- **THEN** the service emits ordered partial content chunks through its API
- **AND** emits a final completion event with the aggregated content
- **AND** surfaces errors in the same stream without exposing sensitive provider details

### Requirement: Streaming Cancellation Control
The LLM service SHALL support canceling an in-progress streaming request and signal the stop to consumers.

#### Scenario: Cancellation requested mid-stream
- **WHEN** cancellation is triggered during streaming
- **THEN** the service stops network work promptly
- **AND** emits a cancellation/termination signal so the UI can clear or mark the in-progress response
- **AND** no further partial updates are emitted after cancellation
