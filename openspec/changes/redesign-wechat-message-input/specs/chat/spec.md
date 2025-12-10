## MODIFIED Requirements

### Requirement: Enhanced Message Input Controls
The system SHALL provide intuitive controls for switching between text, voice, and photo input modes with a WeChat-style compose bar.

#### Scenario: Input mode switching
- **WHEN** the user taps the mode toggle on the compose bar
- **THEN** the bar switches between text mode (text field + send) and voice mode (press-and-hold voice button)
- **AND** the switch animates without jank and preserves the current typed text when returning to text mode

#### Scenario: Hold-to-talk voice capture
- **WHEN** the user presses and holds the voice button in voice mode
- **THEN** recording starts immediately with visual feedback and a “slide to cancel” affordance
- **AND** releasing the button sends the voice message, while sliding out cancels and discards the recording

#### Scenario: Quick attachments
- **WHEN** the user taps the plus/attach button
- **THEN** options for camera capture and gallery selection appear in a compact sheet
- **AND** selecting an option triggers the existing permission and media workflows

#### Scenario: Emoji/quick insert
- **WHEN** the user taps the emoji button in text mode
- **THEN** an emoji picker opens without obscuring the send button
- **AND** inserting emoji updates the text field without losing cursor position

#### Scenario: Media input validation
- **WHEN** the user attempts to send media from the redesigned bar
- **THEN** required permissions (camera, microphone, gallery) are validated
- **AND** clear error messages are shown if permissions are denied
- **AND** an option to open settings is provided
