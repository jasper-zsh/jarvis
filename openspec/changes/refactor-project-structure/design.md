## Context

The Jarvis Android project has grown organically from a simple chat application to a complex voice and real-time communication system with multiple integrations:

- **Current State**: Mixed architecture with some Clean Architecture patterns but inconsistent application
- **Recent Additions**: PipeCat integration, WebRTC real-time communication, Rokid glasses connection, audio processing
- **Pain Points**: Scattered functionality, unclear dependencies, difficult testing, cognitive overhead for new developers

## Goals / Non-Goals

**Goals:**
- Establish clear architectural boundaries following Clean Architecture principles
- Improve code organization and discoverability
- Enable easier testing and maintenance
- Support future feature development with clear extension points
- Reduce cognitive load for developers

**Non-Goals:**
- Complete rewrite of existing functionality
- Introduction of new frameworks or heavy dependencies
- Changes to external APIs or public interfaces
- Performance optimization (though may be a side benefit)

## Decisions

### 1. Package Structure Organization
**Decision**: Adopt a hybrid approach combining layer-based and feature-based organization.

**Rationale**:
- Pure layer-based separation leads to fragmented feature code
- Pure feature-based separation can duplicate cross-cutting concerns
- Hybrid approach provides both feature cohesion and layer clarity

**New Structure**:
```
pro/sihao/jarvis/
├── core/                    # Cross-cutting concerns
│   ├── domain/             # Business logic and models
│   ├── data/               # Data layer (repositories, datasources)
│   └── presentation/       # Base UI components and utilities
├── features/               # Feature modules
│   ├── chat/               # Chat functionality
│   ├── realtime/           # Real-time voice/video communication
│   ├── media/              # Media handling and processing
│   ├── glasses/            # Rokid glasses integration
│   └── settings/           # Application settings
├── platform/               # Platform-specific implementations
│   ├── android/            # Android-specific utilities
│   ├── network/            # Network and connectivity
│   └── security/           # Encryption and security
└── di/                     # Dependency injection modules
```

### 2. Dependency Direction Enforcement
**Decision**: Enforce strict dependency rules: `features → core → platform`

**Rationale**:
- Prevents circular dependencies
- Ensures core business logic remains platform-agnostic
- Makes testing easier by isolating concerns
- Enables potential future multi-platform support

### 3. Module Granularity
**Decision**: Start with package-level organization, prepare for future module migration

**Rationale**:
- Package restructuring is less disruptive than immediate module separation
- Allows gradual migration to Gradle modules if needed
- Maintains single-build simplicity while improving organization

### 4. Naming Conventions
**Decision**: Use clear, descriptive names that reflect responsibility

**Rationale**:
- Improves code discoverability
- Reduces cognitive overhead for new team members
- Makes architecture more self-documenting

## Risks / Trade-offs

**Risk**: Massive refactoring may introduce bugs
- **Mitigation**: Comprehensive testing, incremental migration, automated refactoring tools

**Risk**: Temporary merge conflicts during transition
- **Mitigation**: Clear communication, feature flags, coordination with team

**Trade-off**: Increased initial development effort
- **Benefit**: Long-term maintainability and development velocity

**Trade-off**: Learning curve for new structure
- **Benefit**: Once learned, development becomes more intuitive

## Migration Plan

### Phase 1: Core Structure Setup
1. Create new package directories
2. Move domain models and business rules to `core/domain`
3. Set up dependency injection modules for new structure
4. Update imports for core components

### Phase 2: Feature Migration
1. Migrate chat feature to `features/chat`
2. Migrate realtime feature to `features/realtime`
3. Migrate media handling to `features/media`
4. Migrate glasses integration to `features/glasses`
5. Migrate settings to `features/settings`

### Phase 3: Platform Layer Organization
1. Move Android-specific code to `platform/android`
2. Organize network components in `platform/network`
3. Consolidate security components in `platform/security`

### Phase 4: Cleanup and Validation
1. Remove old empty packages
2. Update documentation
3. Run comprehensive tests
4. Update build configurations

## Open Questions

- Should we introduce Gradle modules immediately or start with packages only?
- How do we handle shared components between features (e.g., message models)?
- What's the timeline for this migration considering ongoing development?
- Do we need to maintain backward compatibility during migration?