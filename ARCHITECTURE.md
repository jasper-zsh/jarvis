# Jarvis Android App Architecture

## Overview

The Jarvis Android app follows Clean Architecture principles with a hybrid layer-based and feature-based organization. This architecture provides clear separation of concerns, improves maintainability, and enables easier testing.

## Package Structure

```
pro/sihao/jarvis/
├── core/                    # Cross-cutting concerns and shared business logic
│   ├── domain/             # Core business logic and models
│   │   ├── model/          # Domain models (Message, MediaInfo, etc.)
│   │   ├── repository/     # Repository interfaces
│   │   └── service/        # Domain services (PipeCatService, etc.)
│   ├── data/               # Core data layer implementations
│   │   ├── database/       # Room database and DAOs
│   │   ├── storage/        # File and media storage
│   │   └── repository/     # Repository implementations
│   └── presentation/       # Shared UI components and utilities
│       ├── theme/          # App theme and styling
│       └── model/          # Shared UI models
├── features/               # Feature-based modules
│   ├── chat/               # Chat functionality
│   │   └── presentation/
│   │       ├── screens/    # Chat screens
│   │       ├── viewmodel/  # Chat ViewModels
│   │       └── components/ # Chat UI components
│   ├── realtime/           # Real-time voice/video communication
│   │   ├── presentation/   # Real-time UI (screens, viewmodels, components)
│   │   └── data/           # Real-time data layer (bridge, config, service)
│   ├── media/              # Media handling and processing
│   │   └── presentation/   # Media UI components
│   ├── glasses/            # Rokid glasses integration
│   │   └── presentation/   # Glasses UI (screens, viewmodels)
│   └── settings/           # Application settings
│       └── presentation/   # Settings UI (screens, viewmodels)
├── platform/               # Platform-specific implementations
│   ├── android/            # Android-specific utilities
│   │   ├── audio/          # Audio processing and routing
│   │   ├── connection/     # Device connections (glasses, etc.)
│   │   ├── media/          # Platform media handling
│   │   ├── permission/     # Permission management
│   │   └── service/        # Android services
│   ├── network/            # Network and connectivity
│   │   └── webrtc/         # WebRTC implementation
│   └── security/           # Security and encryption
│       └── encryption/     # API key and data encryption
└── di/                     # Dependency injection modules
    ├── DatabaseModule.kt   # Database configuration
    ├── NetworkModule.kt    # Network service bindings
    ├── PipeCatModule.kt    # PipeCat dependencies
    └── RepositoryModule.kt # Repository bindings
```

## Architecture Principles

### 1. Dependency Direction
- **Features** depend on **Core** and **Platform**
- **Core** contains business logic and is platform-agnostic
- **Platform** contains technical infrastructure and platform-specific code
- Dependencies flow: `features → core → platform`

### 2. Layer Responsibilities

#### Core Layer
- **Domain**: Business rules, entity models, use case interfaces
- **Data**: Repository implementations, database, storage
- **Presentation**: Shared UI components, themes, utilities

#### Feature Modules
- Contain all components needed for a specific feature
- Have their own presentation layer (screens, ViewModels, components)
- May have feature-specific data implementations
- Depend on core for shared business logic

#### Platform Layer
- **Android**: Platform-specific implementations (permissions, services, audio)
- **Network**: Network abstractions and WebRTC implementation
- **Security**: Encryption, API key management

### 3. Clean Architecture Benefits

1. **Testability**: Business logic is isolated from platform dependencies
2. **Maintainability**: Clear separation makes code easier to understand and modify
3. **Extensibility**: New features can be added without affecting existing code
4. **Platform Independence**: Core logic can potentially be shared across platforms

## Migration Notes

This architecture was implemented as part of the `refactor-project-structure` change. The migration involved:

1. **Package Reorganization**: Moving files from the old mixed structure to the new layered structure
2. **Import Updates**: Updating all package declarations and import statements
3. **Dependency Updates**: Modifying DI modules to work with the new structure
4. **Build Configuration**: Updating AndroidManifest.xml and build files

### Next Steps

1. **Complete Import Updates**: Some import statements may still need manual updates
2. **Testing**: Run comprehensive tests to ensure all functionality works
3. **Documentation**: Update any remaining inline documentation
4. **Gradual Refinement**: Continue improving the structure as needed

## Development Guidelines

### Adding New Features

1. Create a new feature module under `features/`
2. Follow the established structure: `presentation/{screens,viewmodel,components}`
3. If feature needs data access, add `data/` package
4. Use core for shared business logic and models
5. Use platform for technical infrastructure

### Working with Existing Features

1. **Chat**: All chat-related UI in `features/chat/presentation/`
2. **Real-time**: Real-time communication in `features/realtime/`
3. **Glasses**: Rokid glasses integration in `features/glasses/`
4. **Settings**: App configuration in `features/settings/`

### Shared Components

- Use `core/presentation/` for truly shared UI components
- Use `core/domain/` for shared business logic
- Use `platform/` for technical infrastructure

This architecture provides a solid foundation for future development while maintaining clean separation of concerns and enabling easier testing and maintenance.