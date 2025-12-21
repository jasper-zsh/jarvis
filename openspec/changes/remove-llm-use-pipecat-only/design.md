# Architecture Design: Remove LLM, Use PipeCat Only

## Current Architecture
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   ChatScreen    │◄──►│   ChatViewModel  │◄──►│   LLMService    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │ MessageRepo      │    │ ProviderRepo    │
                       └──────────────────┘    └─────────────────┘
                                                        │
                                                        ▼
                                               ┌─────────────────┐
                                               │ ModelConfigRepo │
                                               └─────────────────┘

┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│RealTimeCallScreen│◄──►│ PipeCatViewModel │◄──►│ PipeCatService  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## Target Architecture
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   ChatScreen    │◄──►│   ChatViewModel  │◄──►│ PipeCatService  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │ MessageRepo      │    │ PipeCatConfig   │
                       └──────────────────┘    └─────────────────┘

┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│RealTimeCallScreen│◄──►│ PipeCatViewModel │◄──►│ PipeCatService  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## Key Changes

### 1. Service Layer Consolidation
**Before:**
- `LLMService` handles text chat
- `PipeCatService` handles real-time voice only
- Separate configuration systems

**After:**
- `PipeCatService` handles all chat modes (text, voice, media)
- Unified configuration system
- Single conversation history

### 2. Configuration Simplification
**Removed:**
- `LLMProviderEntity` and related database tables
- `ModelConfigEntity` and related database tables
- Provider management UI screens
- Model selection components
- API key management system

**Added/Enhanced:**
- Simplified PipeCat configuration
- Connection settings only (server URL, auth token)

### 3. ViewModel Updates
**ChatViewModel changes:**
- Remove `LLMService` dependency
- Add `PipeCatService` dependency
- Remove provider/model state management
- Remove API key checking logic
- Update message sending to use PipeCat

**Removed ViewModels:**
- `ProviderConfigViewModel`
- `ProviderListViewModel`
- `ModelTestViewModel`

### 4. UI Component Removal
**Removed screens:**
- `ProviderConfigScreen`
- `ProviderListScreen`
- `ModelConfigScreen`
- `ModelSelectorScreen`
- API setup prompts

**Removed components:**
- `ModelSwitcher`
- `ProviderHealthIndicator`
- `ModelTestDialog`

### 5. Database Schema Changes
**Removed tables:**
- `llm_providers`
- `model_configs`

**Simplified message storage:**
- Remove LLM provider references
- Simplify message entity structure

## Data Flow

### Text Chat Flow (New)
```
User Input → ChatViewModel.sendMessage() → PipeCatService → PipeCat Server → Response → ChatViewModel → UI
```

### Voice Chat Flow (Existing, enhanced)
```
Voice Input → PipeCatViewModel → PipeCatService → PipeCat Server → Response → UI
```

### Media Handling
```
Media Capture → Processing → PipeCatService → PipeCat Server → Analysis → Response
```

## Error Handling Strategy

### Connection Errors
- Detect PipeCat connectivity issues
- Provide retry mechanisms
- Graceful fallback to offline mode if applicable

### Message Processing Errors
- Handle PipeCat API errors
- Provide user-friendly error messages
- Maintain conversation history during failures

## Migration Strategy

### Database Migration
1. Remove LLM-related tables
2. Migrate existing messages (remove provider references)
3. Add new PipeCat configuration table if needed

### User Data Migration
- Export existing conversation history
- Preserve media files
- Remove provider/model settings from preferences

### Configuration Migration
- Remove all provider/API key configurations
- Set default PipeCat server settings
- Initialize new configuration structure

## Security Considerations

### Removed Security Concerns
- API key storage and encryption
- Provider credential management
- Model access control

### New Security Considerations
- PipeCat server authentication
- Media file handling security
- Connection encryption for PipeCat communication

## Performance Implications

### Positive
- Reduced memory footprint (fewer services)
- Faster startup (no provider initialization)
- Simplified dependency injection

### Potential Issues
- Single point of failure (PipeCat only)
- Network dependency for all chat functionality
- Need robust connection management

## Testing Strategy

### Unit Tests
- PipeCat service integration tests
- ViewModel behavior tests
- Message repository tests

### Integration Tests
- End-to-end chat flows
- Media message handling
- Error scenario testing

### UI Tests
- Chat screen interactions
- Settings screen functionality
- Error state handling