## ADDED Requirements
### Requirement: Simplified glasses audio integration
Glasses SHALL integrate with realtime chat as a standard Bluetooth audio device, using Android's built-in audio routing without custom processing or bridge components.

#### Scenario: Glasses as Bluetooth headphone
- **WHEN** glasses are connected via Bluetooth and realtime chat is started
- **THEN** Android automatically routes microphone input through glasses and audio output through glasses speakers

#### Scenario: Audio fallback behavior
- **WHEN** glasses are disconnected during an active realtime session
- **THEN** audio input/output automatically falls back to phone microphone and speaker without session interruption

## REMOVED Requirements
### Requirement: Custom audio routing manager
**Reason**: AudioRoutingManager adds unnecessary complexity for what Android AudioManager handles automatically
**Migration**: Remove AudioRoutingManager and rely on Android's standard Bluetooth SCO audio routing

### Requirement: Glasses-PipeCat audio bridge
**Reason**: GlassesPipeCatBridge creates unnecessary indirection between glasses audio and realtime processing
**Migration**: Let realtime chat components directly access audio from standard Android audio system

### Requirement: Custom TTS integration
**Reason**: Custom TTS sending to glasses duplicates functionality already handled by realtime audio output
**Migration**: Use standard realtime audio output which automatically routes through connected Bluetooth devices