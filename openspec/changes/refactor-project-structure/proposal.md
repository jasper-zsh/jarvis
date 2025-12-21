# Change: Refactor Project Structure for Clear Architecture

## Why
The current project structure has evolved organically with mixed concerns, making it difficult to maintain and extend. Components are scattered across multiple layers without clear separation of responsibilities, and the addition of new capabilities like PipeCat integration has created architectural inconsistencies.

## What Changes
- Reorganize package structure following Clean Architecture principles with clear layer separation
- Consolidate related functionality into cohesive modules (chat, realtime, media, connection)
- Establish clear dependency directions (domain → data → presentation)
- Create feature-based modules for better maintainability
- **BREAKING**: All package imports will need to be updated across the codebase

## Impact
- Affected specs: All existing capabilities will benefit from clearer structure
- Affected code: All Kotlin source files will need package restructuring
- Build system: Gradle configuration will need updates for new module structure
- Development: Improved code navigation, testing, and feature development