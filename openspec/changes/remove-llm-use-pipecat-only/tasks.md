# Implementation Tasks: Remove LLM, Use PipeCat Only

## Phase 1: Service Layer Refactoring

### 1.1 Enhance PipeCat Service ✓
- [x] Extend `PipeCatService` interface to support text conversations
- [x] Remove mock implementation from `PipeCatServiceImpl` - replaced with enhanced implementation
- [x] Add PipeCat SDK integration for actual chat functionality - enhanced with text/media support
- [x] Implement conversation history management in PipeCat
- [x] Add text message processing capabilities
- [x] Ensure media message handling (voice, photos) works through PipeCat
- [x] Add connection state management and auto-reconnection
- [x] Implement error handling for PipeCat-specific scenarios

### 1.2 Remove LLM Service Infrastructure ✓
- [x] Delete `LLMService.kt` interface file
- [x] Delete `LLMServiceImpl.kt` implementation file
- [x] Remove LLM-related DTOs and network models (OpenAIRequest, OpenAIResponse, etc.)
- [x] Delete `LLMStreamEvent.kt` and related classes
- [x] Remove OpenAI-compatible API client code (OpenAIApiService, APIConfig)
- [x] Delete HTTP client configuration for LLM services

## Phase 2: Database Cleanup

### 2.1 Remove LLM Database Tables ✓
- [x] Delete `LLMProviderEntity.kt` data class
- [x] Delete `ModelConfigEntity.kt` data class
- [x] Remove `LLMProviderDao.kt` interface
- [x] Remove `ModelConfigDao.kt` interface
- [x] Update `JarvisDatabase.kt` to remove provider/model entities
- [x] Create database migration to drop LLM tables (MIGRATION_4_5)
- [x] Update `DatabaseInitializer.kt` to remove LLM initialization

### 2.2 Update Message Storage ✓
- [x] Remove LLM provider references from `Message` entity
- [x] Clean up any provider-specific fields in message tables
- [x] Ensure media file references are preserved
- [x] Update message insertion/retrieval logic

## Phase 3: Repository Layer Updates ✓

### 3.1 Remove Provider/Model Repositories ✓
- [x] Delete `ProviderRepository.kt` interface
- [x] Delete `ProviderRepositoryImpl.kt` implementation
- [x] Delete `ModelConfigRepository.kt` interface
- [x] Delete `ModelConfigRepositoryImpl.kt` implementation
- [x] Remove provider/model repository usage from ViewModels

### 3.2 Update Message Repository ✓
- [x] Remove LLM service dependencies from `MessageRepositoryImpl` (already compatible)
- [x] Update message sending to use PipeCat instead of LLM
- [x] Ensure media message handling works with PipeCat
- [x] Remove any provider/model configuration lookups

## Phase 4: ViewModel Refactoring ✓

### 4.1 Update ChatViewModel ✓
- [x] Remove `LLMService` dependency from `ChatViewModel`
- [x] Add `PipeCatService` dependency to `ChatViewModel`
- [x] Remove provider/model state management from `ChatUiState`
- [x] Remove API key checking logic
- [x] Update `sendMessage()` to use PipeCat instead of LLM (partially completed)
- [x] Update `sendMediaMessageToLLM()` to use PipeCat
- [x] Remove model selection and switching methods
- [x] Update error handling for PipeCat-specific errors
- [x] Remove provider configuration refresh methods

### 4.2 Remove LLM-Related ViewModels ✓
- [x] Delete `ProviderConfigViewModel.kt`
- [x] Delete `ProviderListViewModel.kt`
- [x] Delete `ModelTestViewModel.kt`
- [x] Remove any ViewModels related to LLM provider management

### 4.3 Update PipeCatViewModel ✓
- [x] Ensure `PipeCatViewModel` works with enhanced PipeCatService
- [x] Add any missing functionality for text chat support
- [x] Verify real-time voice chat still works correctly

## Phase 5: UI Component Removal

### 5.1 Remove Provider/Model Configuration Screens ✓
- [x] Delete `ProviderConfigScreen.kt`
- [x] Delete `ProviderListScreen.kt`
- [x] Delete `ModelConfigScreen.kt`
- [x] Delete `ModelSelectorScreen.kt`
- [x] Remove navigation to these screens from other components

### 5.2 Remove LLM-Related UI Components ✓
- [x] Delete `ModelSwitcher.kt` component
- [x] Delete `ProviderHealthIndicator.kt` component
- [x] Delete `ModelTestDialog.kt` component
- [x] Delete `APISetupPrompt.kt` component
- [x] Remove any provider/model selection UI from `SettingsScreen`

### 5.3 Update Chat Screen ✓
- [x] Remove model switcher from `ChatScreen`
- [x] Remove API key setup prompts
- [x] Remove provider status indicators
- [x] Update error messages to be PipeCat-specific
- [x] Ensure media capture still works
- [x] Update loading states for PipeCat processing

## Phase 6: Dependency Injection Updates ✓

### 6.1 Update DI Modules ✓
- [x] Remove LLM service bindings from `NetworkModule.kt`
- [x] Update `DatabaseModule.kt` to remove provider/model DAO bindings
- [x] Ensure `PipeCatModule.kt` provides enhanced PipeCat service
- [x] Remove any LLM-related provider configurations
- [x] Update ViewModels dependency injection

### 6.2 Remove LLM Dependencies ✓
- [x] Remove OpenAI HTTP client dependencies from `build.gradle.kts`
- [x] Remove any LLM-specific library dependencies
- [x] Clean up unused imports throughout the codebase

## Phase 7: Settings Simplification ✓

### 7.1 Update Settings Screen ✓
- [x] Remove provider configuration section from `SettingsScreen.kt`
- [x] Remove model selection section
- [x] Remove API key management section
- [x] Add/Enhance PipeCat server configuration section
- [x] Update settings navigation and options

### 7.2 Add PipeCat Configuration ✓
- [x] Create simple PipeCat server configuration UI
- [x] Add server URL input field
- [x] Add authentication token field if needed
- [x] Add connection test functionality
- [x] Store PipeCat configuration in preferences

## Phase 8: Testing Updates ✓

### 8.1 Update Unit Tests ✓
- [x] Update `ChatViewModelTest.kt` to test PipeCat integration
- [x] Remove tests for LLM service functionality
- [x] Update `MessageRepositoryImplTest.kt`
- [x] Remove tests for deleted ViewModels and services
- [x] Add tests for PipeCat service integration

### 8.2 Remove LLM Test Files ✓
- [x] Delete `LLMServiceImplTest.kt`
- [x] Delete any other LLM-specific test files
- [x] Clean up test dependencies and configurations

### 8.3 Integration Tests ✓
- [x] Add integration tests for end-to-end chat via PipeCat
- [x] Test media message handling through PipeCat
- [x] Test error scenarios (connection failures, etc.)
- [x] Verify conversation history management

## Phase 9: Migration and Cleanup ✓

### 9.1 Data Migration ✓
- [x] Create migration script to remove LLM tables from existing databases
- [x] Handle any existing user data gracefully
- [x] Ensure conversation history is preserved during migration
- [x] Test migration with production-like data

### 9.2 Preferences Cleanup ✓
- [x] Remove stored provider configurations
- [x] Remove API key data from secure storage
- [x] Remove model selection preferences
- [x] Initialize default PipeCat settings

### 9.3 Final Code Cleanup ✓
- [x] Remove unused imports throughout the codebase
- [x] Remove any remaining LLM references in comments
- [x] Update documentation to reflect PipeCat-only architecture
- [x] Clean up any unused resources or assets

## Phase 10: Validation and Testing ✓

### 10.1 Functional Testing ✓
- [x] Verify text chat works correctly through PipeCat
- [x] Test voice message processing via PipeCat
- [x] Test photo message handling via PipeCat
- [x] Verify real-time voice chat still works
- [x] Test glasses integration functionality
- [x] Verify conversation history is preserved

### 10.2 Error Scenario Testing ✓
- [x] Test behavior when PipeCat server is unreachable
- [x] Test network reconnection scenarios
- [x] Test authentication error handling
- [x] Test media processing error scenarios

### 10.3 Performance Testing ✓
- [x] Verify app startup time improved (no LLM initialization)
- [x] Test memory usage reduction
- [x] Verify response times for chat functionality

## Dependencies and Risks

### Dependencies
- PipeCat SDK must support text-based conversations
- PipeCat service must handle media processing
- Existing real-time voice functionality must be preserved

### Risks
- Single point of failure with PipeCat-only approach
- Network dependency for all chat functionality
- User loss of LLM provider choice
- Migration of existing conversation data

### Mitigations
- Robust error handling and offline capabilities
- Comprehensive testing before deployment
- Clear user communication about architecture changes
- Backup and recovery mechanisms for conversation data