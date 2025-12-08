# Change: Add API Settings Database Migration

## Why
The current implementation uses EncryptedSharedPreferences to store API settings, which limits data organization, query capabilities, and scalability. Moving to a Room database will enable structured storage of multiple providers, model configurations, and user preferences while maintaining security through existing encryption patterns.

## What Changes
- **Database Schema**: Create new entities for LLMProvider and ModelConfig
- **Migration**: Move existing SharedPreferences settings to Room database
- **Provider Management**: Add UI for managing multiple LLM providers
- **Model Selection**: Enable dynamic model selection in chat interface
- **Settings Enhancement**: Improve settings UI with provider/model management

**BREAKING**: Database schema version change (v1 → v2) with migration logic

## Impact
- Affected specs:
  - New: `provider-management`, `model-manager-ui`, `chat-model-selection`
- Affected code:
  - `JarvisDatabase.kt` - Add new entities and DAOs
  - `SecureStorage.kt` - Refactor to use database
  - `LLMServiceImpl.kt` - Update configuration loading
  - Settings and Chat UI components - Add provider/model selection