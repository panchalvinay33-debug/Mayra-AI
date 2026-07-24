# Mayra AI

Mayra AI is a voice-first, memory-enabled, context-aware Personal Intelligence System and Digital Companion for Android, built with Kotlin and Jetpack Compose.

## Locked product reference

All future architecture, UI, features, tests and GitHub development must follow:

- [`docs/MAYRA_AI_MASTER_BLUEPRINT.md`](docs/MAYRA_AI_MASTER_BLUEPRINT.md)

The blueprint preserves the complete existing concept and merges the Phone & App Control System, messaging, notification intelligence, conditional call-assistant roadmap, permission/risk architecture, privacy rules, phased roadmap and current-code gap analysis.

## Current implemented foundations

- Local and OpenAI hybrid assistant boundary
- Text chat and continuous voice conversation
- Hindi, Hinglish and English-ready settings
- Animated living Mayra presence and Phone Pulse
- Context, memory, personal intelligence and knowledge foundations
- Agent, autonomy, workflow and plugin foundations
- Android device-action specifications and safe execution boundaries
- Notification listener and runtime-attention system
- Vision/image understanding foundations
- Privacy center and encrypted provider secrets
- Execution control plane, supervisor, adaptive scheduler and recovery
- Runtime dashboard, approvals, workflow history and diagnostics
- Settings, onboarding and permission readiness
- Deterministic unit tests and Android CI/lint gates

## Important platform boundaries

Mayra never guarantees unsupported third-party app automation. Official Android intents/APIs and app-provided integrations are preferred. Full incoming-call control is conditional on default-dialer/InCallService requirements. Sensitive actions require confirmation, and restricted permissions/features must remain Play Store compliant.

## First build

1. Clone/open this repository in Android Studio.
2. Use JDK 17 and Android SDK 35.
3. Allow Gradle sync to finish.
4. Run a debug build on a physical Android device where possible.
5. Complete onboarding and grant permissions only when the relevant feature is used.
6. Configure an AI provider only if online intelligence is required; no API secrets are committed to the repository.

Physical-device validation is still required for OEM-specific voice, notification, Telecom, media and background behavior.
