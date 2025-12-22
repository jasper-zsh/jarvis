## Context

The current PipeCat implementation runs entirely within the main app context using Dagger-Hilt dependency injection. This approach works well when the app is in foreground but breaks when the app loses focus because:

1. Android's process lifecycle management may kill or suspend app processes
2. Audio recording permissions are automatically revoked when app goes to background
3. Network connections may be interrupted to conserve resources
4. User experience is disrupted for voice-based interactions with glasses

The goal is to maintain continuous voice interaction capabilities even when the app is not the active foreground application.

## Goals / Non-Goals

**Goals:**
- Maintain continuous PipeCat connection and audio recording when app loses focus
- Provide seamless voice interaction through glasses regardless of app state
- Comply with Android foreground service requirements and user notifications
- Ensure proper resource management and cleanup
- Maintain backward compatibility with existing PipeCat functionality

**Non-Goals:**
- Complete app background operation (only PipeCat connection needs to continue)
- Major architectural overhaul of existing PipeCat service implementation
- Support for older Android versions lacking proper foreground service support

## Decisions

### Decision: Foreground Service Architecture
- **Approach**: Create a dedicated `PipeCatForegroundService` that hosts the PipeCat connection when the app needs background operation
- **Why**:
  - Android requires foreground services for continuous audio recording in background
  - Provides clear separation of concerns between app UI and background processing
  - Allows proper notification management for user awareness
  - Maintains existing Dagger-Hilt integration patterns
- **Alternatives considered**:
  - **WorkManager**: Not suitable for real-time, continuous audio streams
  - **Regular Service**: Would be killed by system when app goes to background
  - **Bound Service**: Would still be tied to app lifecycle

### Decision: Always-Foreground Service Pattern
- **Approach**: Always host PipeCat connections in a foreground service context, regardless of app focus state
- **Why**:
  - Eliminates complexity of context switching and session migration
  - Guarantees continuous operation without interruption during app lifecycle changes
  - Simplifies architecture by having single execution context for all PipeCat operations
  - Ensures consistent behavior across all app states
- **Implementation**: All PipeCat session initiations will start the foreground service and establish connections within that context

### Decision: Service Integration Strategy
- **Approach**: Refactor existing `PipeCatServiceImpl` to always operate within the foreground service context rather than complete rewrite
- **Why**:
  - Preserves existing investment in current implementation
  - Reduces risk of introducing new bugs
  - Maintains existing API contracts while ensuring consistent background operation
  - Eliminates need for dual-mode complexity

## Risks / Trade-offs

### Risk: Foreground Service Permission Complexity
- **Risk**: Users may deny foreground service permissions, preventing background operation
- **Mitigation**: Implement graceful fallback to app-only operation with clear user communication

### Risk: Battery Impact
- **Risk**: Continuous foreground service may impact battery life
- **Mitigation**:
  - Only activate when needed (active voice session + app backgrounded)
  - Implement efficient resource management
  - Provide clear user controls for disabling background operation

### Risk: System Resource Management
- **Risk**: Foreground services are prioritized but still subject to system limitations
- **Mitigation**:
  - Implement proper error handling for service termination
  - Add auto-retry mechanisms with exponential backoff
  - Provide clear user feedback when service is interrupted

### Trade-off: Complexity vs Reliability
- **Trade-off**: Increased architectural complexity for improved reliability
- **Decision**: Accept complexity as necessary for core functionality (continuous voice interaction)

## Migration Plan

### Phase 1: Foundation
1. Create `PipeCatForegroundService` with basic structure
2. Add notification system and manifest permissions
3. Implement basic lifecycle management

### Phase 2: Integration
1. Refactor `PipeCatServiceImpl` for dual-mode operation
2. Implement context switching logic
3. Add proper cleanup and error handling

### Phase 3: Testing & Polish
1. Comprehensive testing across different scenarios
2. Performance optimization and battery usage analysis
3. User experience refinement and edge case handling

### Rollback Strategy
- Keep existing implementation as fallback
- Feature flag for enabling/disabling foreground service mode
- Ability to quickly disable foreground service if critical issues discovered

## Open Questions

- **Notification Design**: What information should be displayed in the foreground service notification for optimal user awareness without being intrusive?
- **Timeout Strategy**: Should there be an inactivity timeout to automatically stop the foreground service, or should it run until explicitly stopped?
- **Multi-device Support**: How should the system handle multiple Bluetooth devices or when switching between glasses and regular audio?
- **Error Recovery**: What are the best practices for recovering from foreground service termination while maintaining user session context?

## Technical Constraints

### Android Platform Constraints
- Minimum SDK level 28 (Android 9) - supports foreground services
- Notification requirements differ across Android versions
- Audio recording permissions have varying requirements by API level

### Performance Constraints
- Must maintain real-time audio processing latency below 200ms
- Memory usage should not exceed 50MB additional overhead
- Battery impact should be minimal when idle

### Integration Constraints
- Must maintain compatibility with existing Dagger-Hilt dependency graph
- Cannot break existing PipeCat service API contracts
- Must integrate cleanly with current glasses connection management