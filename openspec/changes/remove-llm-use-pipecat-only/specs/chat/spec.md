# Chat Service Specification

## ADDED Requirements

### Requirement: PipeCat Text Chat Support
The system SHALL extend PipeCat service to handle text-based conversations in addition to existing voice functionality.

#### Scenario: Text Message via PipeCat
- **GIVEN** User is on chat screen with PipeCat connection established
- **WHEN** User types "Hello, how are you?" and sends message
- **THEN** Message is sent to PipeCat service for processing
- **AND** Response is received from PipeCat and displayed in chat
- **AND** Message is stored in conversation history

#### Scenario: Text Chat with Context
- **GIVEN** User has existing conversation with 5 previous exchanges
- **WHEN** User sends "What did we discuss earlier?"
- **THEN** PipeCat receives full conversation context
- **AND** Response references previous conversation content appropriately
- **AND** Context is maintained across session restarts

### Requirement: Unified Media Processing
The system SHALL process all media messages (voice, photos) through PipeCat service instead of LLM providers.

#### Scenario: Voice Message via PipeCat
- **GIVEN** User records and sends voice message
- **WHEN** Voice message processing completes
- **THEN** Audio file is sent to PipeCat service for analysis
- **AND** PipeCat provides transcription and text response
- **AND** Both transcription and response appear in chat interface

#### Scenario: Photo Analysis via PipeCat
- **GIVEN** User captures and sends photo
- **WHEN** Photo processing completes
- **THEN** Image file is sent to PipeCat service for analysis
- **AND** PipeCat analyzes image content and provides response
- **AND** Response appears in chat with appropriate image context

### Requirement: PipeCat Connection Management
The system SHALL implement robust connection management for PipeCat service with auto-reconnection and error handling.

#### Scenario: Automatic Reconnection
- **GIVEN** PipeCat connection is established
- **WHEN** Network connection is lost for 30 seconds
- **THEN** PipeCat service automatically detects connection loss
- **AND** Attempts reconnection when network becomes available
- **AND** Restores conversation context upon reconnection
- **AND** User can continue conversation without manual intervention

#### Scenario: Connection Error Handling
- **GIVEN** PipeCat server is unreachable
- **WHEN** User attempts to send message
- **THEN** Error message "Unable to connect to PipeCat server" is displayed
- **AND** User is prompted to check network connection
- **AND** Retry option is available
- **AND** Failed message is preserved for retry when connection restored

### Requirement: Simplified Configuration
The system SHALL replace complex LLM provider configuration with simple PipeCat server settings.

#### Scenario: PipeCat Server Configuration
- **GIVEN** User navigates to chat settings
- **WHEN** User enters PipeCat server URL "wss://api.pipecat.example.com"
- **AND** User provides authentication token if required
- **THEN** Connection test is performed
- **AND** Success/failure status is shown
- **AND** Configuration is saved for future sessions

## MODIFIED Requirements

### Requirement: Text Chat Service Implementation
The system SHALL modify text chat functionality to use PipeCat instead of LLM service abstraction.

#### Scenario: Text Conversation Flow
- **GIVEN** User wants to send text message
- **WHEN** User composes and sends message
- **THEN** Message flows through ChatViewModel to PipeCatService
- **AND** PipeCat processes message and returns response
- **AND** Response displayed in chat interface
- **AND** Conversation history updated appropriately

**Previous Implementation**: Messages were routed through LLMService to configurable LLM providers with streaming responses
**New Implementation**: Messages are sent directly to PipeCatService for unified processing

### Requirement: Chat ViewModel Updates
The system SHALL update ChatViewModel to remove LLM dependencies and use PipeCat service.

#### Scenario: ViewModel Message Processing
- **GIVEN** ChatViewModel is initialized
- **WHEN** sendMessage() is called with text content
- **THEN** LLMService is not used (dependency removed)
- **AND** PipeCatService processes the message
- **AND** Streaming response handled through PipeCat events
- **AND** UI state updated appropriately

**Previous Implementation**: ChatViewModel depended on LLMService, ProviderRepository, ModelConfigRepository for message processing
**New Implementation**: ChatViewModel depends only on PipeCatService, MessageRepository for unified chat processing

### Requirement: Settings Interface Simplification
The system SHALL simplify settings interface to remove LLM provider management and focus on PipeCat configuration.

#### Scenario: Settings Navigation
- **GIVEN** User opens app settings
- **WHEN** User navigates to chat configuration
- **THEN** No provider selection options are displayed
- **AND** No model configuration options are displayed
- **AND** No API key management options are displayed
- **AND** Only PipeCat server configuration is available

**Previous Implementation**: Multiple screens for provider configuration, model selection, API key management
**New Implementation**: Single configuration screen for PipeCat server settings

## REMOVED Requirements

### Requirement: LLM Provider Management
The system SHALL remove the ability to configure and use multiple LLM providers.

#### Scenario: Provider Configuration Removal
- **GIVEN** User previously could configure multiple LLM providers
- **WHEN** LLM provider management is removed
- **THEN** Provider configuration screens are deleted
- **AND** Provider database tables are removed
- **AND** Provider selection UI components are removed
- **AND** All provider-related code is eliminated from codebase

### Requirement: Model Configuration System
The system SHALL remove the ability to configure and switch between different AI models.

#### Scenario: Model Selection Removal
- **GIVEN** User previously could select from multiple AI models per provider
- **WHEN** Model configuration system is removed
- **THEN** Model selection UI components are deleted
- **AND** Model configuration database tables are removed
- **AND** Model switching functionality is eliminated
- **AND** Model parameter configuration (temperature, max tokens) is removed

### Requirement: API Key Management
The system SHALL remove encrypted storage and management of API keys for different LLM providers.

#### Scenario: API Key Cleanup
- **GIVEN** User previously stored API keys for multiple LLM providers
- **WHEN** API key management system is removed
- **THEN** All stored API keys are securely deleted
- **AND** API key encryption storage is removed
- **AND** API key configuration UI is eliminated
- **AND** API key validation logic is removed