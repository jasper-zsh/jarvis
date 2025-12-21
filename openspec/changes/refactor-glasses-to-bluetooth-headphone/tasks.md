## 1. Analysis and Planning
- [x] 1.1 Document current audio routing dependencies and usage patterns
- [x] 1.2 Identify all components that depend on AudioRoutingManager
- [x] 1.3 Map out current audio flow from glasses to realtime chat

## 2. Refactor GlassesConnectionManager
- [x] 2.1 Remove custom audio stream listener and audio processing logic
- [x] 2.2 Simplify connection management to basic Bluetooth device handling
- [x] 2.3 Remove TTS content sending functionality
- [x] 2.4 Keep only device discovery, connection, and basic state management

## 3. Update Realtime Audio Pipeline
- [x] 3.1 Modify realtime chat to detect glasses as standard Bluetooth audio device
- [x] 3.2 Update audio input selection to prefer glasses microphone when connected
- [x] 3.3 Configure audio output to route through glasses speakers automatically
- [x] 3.4 Remove custom audio routing logic and use Android AudioManager

## 4. Remove Complex Audio Components
- [x] 4.1 Delete AudioRoutingManager.kt and all its dependencies
- [x] 4.2 Delete GlassesPipeCatBridge.kt and consolidate any unique functionality
- [x] 4.3 Update dependency injection to remove removed components
- [x] 4.4 Clean up any references in ViewModels and UI components

## 5. Update Configuration and Settings
- [x] 5.1 Simplify glasses settings to basic connection management only
- [x] 5.2 Remove complex audio routing configuration options
- [x] 5.3 Update UI to reflect simplified glasses functionality

## 6. Testing and Validation
- [x] 6.1 Test glasses connection and basic audio I/O in realtime chat
- [x] 6.2 Verify audio routing works correctly with glasses as Bluetooth headset
- [x] 6.3 Test fallback to phone microphone when glasses disconnected
- [x] 6.4 Validate no regressions in standard realtime chat functionality

## 7. Documentation and Cleanup
- [x] 7.1 Update architecture documentation to reflect simplified audio flow
- [x] 7.2 Remove any unused audio-related configuration files
- [x] 7.3 Update code comments to reflect new simplified approach