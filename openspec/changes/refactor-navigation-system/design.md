# Navigation System Refactoring Design

## Context
Current navigation uses a single NavHost with manual navigation between screens (realtime → settings → glasses). This creates tight coupling between screens and scattered navigation logic. Navigation callbacks are passed deep into component hierarchies, making the system hard to maintain and extend.

## Goals / Non-Goals
**Goals:**
- Provide intuitive bottom tab navigation for primary modules
- Centralize navigation logic and state management
- Enable easy addition of new tabs/modules
- Maintain existing screen functionality while improving UX
- Support deep linking and tab restoration

**Non-Goals:**
- Complete rewrite of existing screen content
- Adding complex nested navigation within tabs
- Supporting tablet-specific navigation patterns

## Decisions

### 1. Bottom Tab Navigation Architecture
**Decision**: Use Jetpack Compose Navigation with BottomNavigation scaffold
**Rationale**:
- Native Compose integration, consistent with existing codebase
- Built-in support for backstack handling and deep linking
- Familiar pattern for Android users
- Easy to extend with additional tabs

**Alternatives considered:**
- Custom tab implementation (more control but more maintenance)
- Navigation drawer (less discoverable for primary modules)
- Navigation Compose with nested NavHosts (overly complex for current needs)

### 2. Centralized Navigation Manager
**Decision**: Create NavigationManager singleton to handle all navigation logic
**Rationale:**
- Single source of truth for navigation state
- Eliminates callback passing between components
- Enables programmatic navigation from anywhere (services, view models)
- Supports navigation testing and debugging

### 3. Tab Registration System
**Decision**: Use sealed class hierarchy for tab definitions
**Rationale:**
- Type-safe tab definitions
- Compile-time verification of tab configurations
- Easy to add new tabs without modifying navigation logic
- Supports tab-specific configuration (icons, labels, deep links)

## Risks / Trade-offs
- **Risk**: Existing screen navigation assumptions may break
  - **Mitigation**: Maintain screen isolation, only change container
- **Risk**: Tab navigation may not suit all future modules
  - **Mitigation**: Design system to support hybrid navigation (tabs + nested navigation)
- **Trade-off**: Bottom tabs take screen real estate
  - **Mitigation**: Auto-hide tabs when appropriate, use scroll-friendly content

## Migration Plan
1. Create navigation infrastructure (NavigationManager, Tab definitions)
2. Implement BottomTabNavigation scaffold
3. Migrate existing screens to work within tab container
4. Remove inter-screen navigation callbacks
5. Update MainActivity to use new navigation system
6. Add deep link support and tab restoration
7. Testing and validation

## Open Questions
- Should settings remain as a separate tab or be accessible within existing tabs?
- How should navigation handle service-initiated navigation (e.g., glasses connection events)?
- Should tabs support badges or notification indicators?