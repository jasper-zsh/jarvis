# Change: Refactor Navigation System to Bottom Tab Architecture

## Why
The current navigation system is a messy collection of nested navigation calls and screen-to-screen navigation logic scattered across different screens. There's no unified navigation pattern, making it difficult to add new modules or maintain consistent user experience. A bottom tab-based navigation system will provide clear visual hierarchy, intuitive module switching, and extensible architecture for future tabs.

## What Changes
- **BREAKING**: Replace current NavHost-based navigation with bottom tab navigation
- **BREAKING**: Remove nested navigation callbacks between screens
- **REFACTOR**: Extract navigation logic into centralized navigation manager
- **ADD**: Bottom tab navigation component with Chat and Glasses tabs
- **ADD**: Navigation state management for tab selection and deep linking
- **ADD**: Extensible tab registration system for future modules
- **MODIFY**: Screen compositions to work within tab container
- **REMOVE**: Navigation button dependencies between screens

## Impact
- **Affected specs**: realtime-voice-chat (accessed via drawer/tab reference), model-manager-ui (settings navigation reference)
- **Affected code**: MainActivity.kt navigation setup, all screen navigation callbacks, screen compositions
- **New components**: BottomTabNavigation, NavigationManager, TabNavigationState
- **Migration**: Existing screen navigation logic will be centralized and simplified