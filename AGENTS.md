<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

# Jarvis
This is a personal agent running in Rayneo AR Glasses. It is basically an Android app, with some special widgets and methods from ARDK for better glasses UI experiences.
Your design must follow the instructions and code snippets in ARDK.
If it is not mentioned in ARDK, you can fallback to normal Android way.

## Examples for Android ARDK
Examples for ARDK is placed under `~/mercuryandroidsdk`, if you cannot find it, ask user to place the example project in correct path.
