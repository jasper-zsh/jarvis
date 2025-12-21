## Context
The current glasses audio implementation has evolved into a complex system with multiple overlapping responsibilities:

- **AudioRoutingManager**: Complex PCM audio processing, level calculation, speaker routing
- **GlassesPipeCatBridge**: Integration layer between glasses and PipeCat with state management
- **GlassesConnectionManager**: Custom audio stream handling, VAD, TTS integration

This complexity was originally designed for custom voice command processing but now conflicts with the realtime chat implementation that uses standard Android audio routing.

## Goals / Non-Goals
- **Goals**:
  - Simplify glasses to act as standard Bluetooth headphone
  - Reduce code complexity and maintenance burden
  - Maintain audio functionality in realtime chat
  - Improve reliability by using Android's standard audio routing
- **Non-Goals**:
  - Custom audio processing or effects
  - Complex TTS integration outside of realtime chat
  - Advanced audio routing beyond basic Bluetooth headset functionality

## Decisions
- **Decision**: Remove AudioRoutingManager and GlassesPipeCatBridge entirely
  - **Rationale**: These components add unnecessary complexity for what Android AudioManager can handle
  - **Alternatives considered**: Simplifying existing components vs complete removal - removal chosen for clean architecture

- **Decision**: Use Android's standard Bluetooth SCO (Synchronous Connection-Oriented) for audio
  - **Rationale**: Standard approach for Bluetooth headsets, built-in support in Android
  - **Alternatives considered**: Custom audio routing via AudioManager APIs - SCO is more reliable

- **Decision**: Consolidate functionality into GlassesConnectionManager
  - **Rationale**: Single responsibility for glasses device management
  - **Alternatives considered**: New simplified manager class - unnecessary indirection

## Risks / Trade-offs
- **Risk**: Audio quality or latency may differ from custom implementation
  - **Mitigation**: Test with various audio configurations and Android audio focus handling
- **Risk**: Loss of fine-grained audio level visualization
  - **Mitigation**: Use Android's standard audio level APIs if needed
- **Trade-off**: Reduced customization in favor of simplicity and reliability

## Migration Plan
1. **Phase 1**: Identify and document all current audio dependencies
2. **Phase 2**: Implement simplified glasses connection management
3. **Phase 3**: Update realtime chat to use standard audio routing
4. **Phase 4**: Remove complex audio components
5. **Phase 5**: Test and validate audio functionality

**Rollback**: Keep original components in a separate branch temporarily for emergency rollback if critical audio functionality is lost.

## Open Questions
- How will Android's automatic audio routing behave with realtime chat audio focus?
- Will standard Bluetooth SCO provide sufficient audio quality for voice input?
- Should we retain any audio level visualization for UI feedback?