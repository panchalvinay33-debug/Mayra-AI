# Mayra AI

Mayra AI is a voice-first, memory-enabled, context-aware **Living Personal Intelligence System and Digital Companion for Android**, built with Kotlin and Jetpack Compose.

## Locked product reference

The single product and engineering source of truth is:

- [`docs/MAYRA_AI_MASTER_BLUEPRINT.md`](docs/MAYRA_AI_MASTER_BLUEPRINT.md)

This file is the **Mayra AI Master Blueprint V2 — Living Companion System**. All future architecture, home-screen design, floating-assistant behaviour, permissions, Accessibility work, phone actions, tests and release decisions must follow it unless the owner explicitly changes the product direction.

The V2 blueprint preserves the original intelligence, memory, voice, reminders, agenda, people, notification and Action Safety concepts, and officially adds:

- minimal animated Living Home;
- organized three-dot navigation;
- guided Mayra Access Journey;
- floating assistive Mayra ball over other apps;
- context-aware cross-app quick actions;
- optional transparent Accessibility Assist Mode;
- owner-first personal build strategy;
- later Play Store hardening profile.

## Current implemented foundations

- Local and OpenAI hybrid assistant boundary
- Text chat and continuous voice conversation
- Hindi, Hinglish and English-ready settings
- Animated Mayra presence and Phone Pulse
- Personal Owner Mode and access-readiness foundations
- Contact identities and relationships
- Mayra-owned reminders, follow-ups and Personal Agenda
- Notification intelligence and supported quick replies
- Context, memory, personal intelligence and knowledge foundations
- Agent, autonomy, workflow and plugin foundations
- Android device-action specifications and safe execution boundaries
- Vision/image understanding foundations
- Privacy center and encrypted provider secrets
- Execution control plane, supervisor, adaptive scheduler and recovery
- Runtime dashboard, approvals, workflow history and diagnostics
- Personal Device Test Center and Windows personal-alpha build scripts

## Current implementation priority

1. Redesign the Living Home and move secondary tools into a three-dot menu.
2. Build the guided Access Journey.
3. Implement Floating Mayra V1 with overlay permission and foreground service.
4. Connect the floating surface to voice, reminders, notifications, identities and Action Safety.
5. Add optional Accessibility context assistance after the transparent floating layer is stable.

## Important platform boundaries

Mayra never guarantees unsupported third-party app automation. Official Android intents/APIs and app-provided integrations are preferred. Accessibility use must be explicitly enabled, visible and deterministic. Full incoming-call control is conditional on default-dialer/InCallService requirements. Sensitive actions require confirmation, and opening a compose screen is never reported as a delivered message.

## Personal alpha build

1. Use branch `batch-12-runtime-control-center`.
2. Use JDK 17 and Android SDK 35.
3. Run `scripts/build-personal-alpha.ps1` on Windows, or build the debug APK from Android Studio.
4. Install with `scripts/install-personal-alpha.ps1`.
5. Use the in-app Personal Device Test Center to record real phone results.

Physical-device validation remains required for OEM-specific voice, overlays, notifications, Accessibility, Telecom, media and background behaviour.
