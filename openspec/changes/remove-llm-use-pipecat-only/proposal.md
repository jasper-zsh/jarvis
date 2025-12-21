# Remove LLM Code, Use PipeCat Only for Chat

## Why
Simplify Jarvis architecture by removing the complex LLM provider management system in favor of a unified PipeCat-only approach. This eliminates maintenance overhead for multiple LLM integrations, reduces codebase size, and provides a consistent chat experience across all modes.

## What Changes
Remove all LLM-related infrastructure including provider management, model configuration, and API key handling. Enhance PipeCat service to handle all chat functionality (text, voice, media) with robust connection management. Simplify settings and UI by removing provider/model selection components.

## Summary
This change removes all LLM-related code from Jarvis and transitions chat functionality to use only PipeCat. This eliminates the complexity of managing multiple LLM providers, API keys, and model configurations in favor of a unified PipeCat-based approach.

## Rationale
- **Simplify architecture**: Remove provider management, API key handling, and model configuration complexity
- **Reduce codebase size**: Eliminate ~30+ files related to LLM services, providers, and configurations
- **Unify chat experience**: PipeCat will handle both text and real-time voice conversations
- **Focus on core functionality**: Remove maintenance overhead for multiple LLM integrations
- **Improve user experience**: Single, consistent chat interface without provider selection

## Current State
The app currently has:
- LLM service abstraction (`LLMService`, `LLMServiceImpl`)
- Provider management system with database tables for providers and model configs
- API key management and encryption
- Model selection and switching functionality
- Settings screens for provider configuration
- PipeCat service exists but is only used for real-time voice mode

## Proposed Changes
1. **Remove LLM infrastructure**:
   - Delete `LLMService` and `LLMServiceImpl`
   - Remove provider management system
   - Delete model configuration system
   - Remove API key management

2. **Enhance PipeCat service**:
   - Extend PipeCat implementation to handle text-based conversations
   - Remove mock implementation and integrate with actual PipeCat SDK
   - Add conversation history management to PipeCat
   - Ensure PipeCat can handle all message types (text, voice, media)

3. **Update UI/ViewModels**:
   - Remove provider/model selection UI components
   - Simplify settings screens
   - Update `ChatViewModel` to use PipeCat instead of LLM service
   - Remove API key setup prompts

4. **Database cleanup**:
   - Remove LLM provider and model config tables
   - Clean up related DAOs and entities

## Impact
- **Positive**: Significant code reduction, simplified architecture, focused development
- **Negative**: Loses flexibility to switch between different LLM providers
- **Migration**: User's existing provider/API key configurations will be removed

## Testing
- Verify text chat works through PipeCat
- Test voice/media message handling
- Ensure glasses integration still functions
- Validate error handling for PipeCat connection issues

## Alternatives Considered
1. **Keep both systems**: Would maintain current flexibility but adds complexity
2. **Gradual migration**: More complex implementation with dual support during transition
3. **Replace with different service**: PipeCat appears to be the strategic choice based on existing investment