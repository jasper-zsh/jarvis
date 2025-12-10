## ADDED Requirements

### Requirement: Clear Conversation History
The system SHALL allow users to clear the current conversation history while preserving other conversations.

#### Scenario: User clears conversation
- **WHEN** user selects the clear conversation action and confirms
- **THEN** all messages for the active conversation are deleted from storage
- **AND** associated media files for that conversation are removed
- **AND** chat UI refreshes to an empty state with a placeholder message

#### Scenario: Cancel clear action
- **WHEN** user dismisses the confirmation dialog
- **THEN** no messages or media are deleted
- **AND** chat UI remains unchanged

#### Scenario: In-flight requests
- **WHEN** there are in-progress LLM or upload requests for the conversation being cleared
- **THEN** those requests are cancelled
- **AND** no partial messages remain in the conversation

#### Scenario: LLM context reset
- **WHEN** conversation is cleared
- **THEN** the associated LLM session/context is reset so subsequent messages start fresh

#### Scenario: Persistence verification
- **WHEN** app is closed and reopened after clearing
- **THEN** cleared conversation remains empty
- **AND** no orphaned media files for that conversation remain on disk
