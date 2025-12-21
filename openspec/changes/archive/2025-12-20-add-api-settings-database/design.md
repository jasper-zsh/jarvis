## Context

The current Jarvis Android app stores API configuration in EncryptedSharedPreferences, including:
- API keys
- Provider selection (OpenAI, DeepSeek, etc.)
- Base URLs for custom endpoints
- Model names and parameters
- Temperature and max tokens settings

This approach works but has limitations:
- No structured relationships between providers and models
- Limited querying capabilities for complex configurations
- Difficult to manage multiple provider setups
- No support for provider-specific model catalogs

## Goals / Non-Goals

**Goals:**
- Migrate from SharedPreferences to Room database for API settings
- Enable multiple provider configurations with proper relationships
- Support dynamic model discovery and selection
- Maintain security through existing encryption patterns
- Provide UI for provider and model management
- Enable model switching in chat interface

**Non-Goals:**
- Real-time model capability detection
- Provider-specific authentication methods beyond API keys
- Model fine-tuning or custom training data
- Multi-tenant account management

## Decisions

**Decision**: Use Room database with proper entity relationships
- **Rationale**: Room provides compile-time verification, LiveData support, and easy migrations
- **Alternatives considered**:
  - Stay with SharedPreferences (limited scalability)
  - Use raw SQLite (error-prone, no compile-time checks)

**Decision**: Create separate LLMProvider and ModelConfig entities
- **Rationale**: Many-to-many relationship between providers and models
- **Alternatives considered**:
  - Single configuration table (denormalized, redundant data)
  - JSON storage in single column (not queryable)

**Decision**: Keep SecureStorage for API keys only
- **Rationale**: API keys are highly sensitive and should remain in EncryptedSharedPreferences
- **Alternatives considered**:
  - Move everything to database (security concerns)
  - Use separate encrypted database (complexity vs benefit)

**Decision**: Database version increment with migration strategy
- **Rationale**: Existing MessageEntity data must be preserved
- **Alternatives considered**:
  - Fresh database (data loss)
  - Separate settings database (unnecessary complexity)

## Risks / Trade-offs

**Risk**: Migration complexity from SharedPreferences to database
- **Mitigation**: Implement proper Room migration with fallback logic

**Risk**: Performance impact of database queries vs SharedPreferences
- **Mitigation**: Cache current configuration in memory, use lazy loading

**Trade-off**: Additional database size vs structured data benefits
- **Justification**: Small data footprint, significant organizational benefits

**Risk**: Breaking changes to existing configuration access
- **Mitigation**: Maintain backward compatibility through wrapper methods during transition

## Migration Plan

1. **Phase 1**: Create new database entities and DAOs
   - Add LLMProvider and ModelConfig entities to JarvisDatabase
   - Increment database version to 2
   - Create migration logic from SharedPreferences

2. **Phase 2**: Implement data access layer
   - Create repositories for provider and model management
   - Update SecureStorage to coordinate with database
   - Add migration service to transfer existing settings

3. **Phase 3**: Update UI components
   - Add provider management screen
   - Create model selection interface
   - Integrate model switching in chat

4. **Phase 4**: Cleanup and optimization
   - Remove deprecated SharedPreferences methods
   - Add database indexing for performance
   - Implement data validation and error handling

**Rollback**: Keep migration logic reversible with fallback to SharedPreferences if database corruption occurs

## Open Questions

- Should we cache provider model catalogs locally to reduce API calls?
- How to handle invalid API keys during migration validation?
- Should we implement provider health checking and automatic failover?
- Do we need user preference profiles for different use cases?