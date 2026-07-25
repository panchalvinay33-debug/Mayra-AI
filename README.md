# Mayra AI

Mayra AI is a voice-first, memory-enabled, context-aware **Living Personal Intelligence System and Digital Companion**, beginning on Android and designed to extend later to smart displays, home devices, vehicles, wearables and spatial or holographic presence surfaces.

## Locked product references

The permanent product and engineering references are:

- [`docs/MAYRA_AI_MASTER_BLUEPRINT.md`](docs/MAYRA_AI_MASTER_BLUEPRINT.md) — Master Blueprint V2 and current Android Living Companion architecture.
- [`docs/MAYRA_LIVING_INTELLIGENCE_VISION.md`](docs/MAYRA_LIVING_INTELLIGENCE_VISION.md) — one-brain/many-bodies future vision for smart devices and holographic presence.
- [`docs/MAYRA_SOURCE_OF_TRUTH_AND_BACKUP_MAP.md`](docs/MAYRA_SOURCE_OF_TRUTH_AND_BACKUP_MAP.md) — source-of-truth, branch, recovery and backup rules.
- [`docs/PERSONAL_ALPHA_STABILIZATION_STATUS.md`](docs/PERSONAL_ALPHA_STABILIZATION_STATUS.md) — current V0.1 evidence, blockers and acceptance gate.

All future architecture, user experience, permissions, actions, tests and releases must preserve these references unless the owner explicitly changes the product direction.

## Product identity

Mayra is not intended to remain a chatbot. Android is the first body. The reusable Mayra intelligence, memory, skills, safety and presence contracts should support future device bodies without duplicating the brain or losing identity.

The Android product currently centres on:

- a minimal animated Living Home;
- Floating Mayra over other apps;
- Hindi, Hinglish and English text/voice interaction;
- reminders, agenda, people, notes, memory and notification intelligence;
- safe app, call and message handoffs;
- Phone Pulse and proactive attention;
- optional online AI with local/offline continuity;
- visible permissions, audit and a global stop switch.

## Current implemented foundations

- Local and OpenAI hybrid assistant boundary
- Text chat and continuous voice foundations
- Animated Mayra presence and Phone Pulse
- Personal Owner Mode and access-readiness foundations
- Contact identities and relationships
- Mayra-owned reminders, follow-ups and Personal Agenda
- Notification intelligence and supported quick replies
- Context, memory, personal intelligence and knowledge foundations
- Agent, autonomy, workflow and plugin foundations
- Android device-action specifications and safe execution boundaries
- Vision/image understanding foundations
- Privacy centre and encrypted provider secrets
- Execution control plane, supervisor, adaptive scheduler and recovery
- Runtime dashboard, approvals, workflow history and diagnostics
- Floating Mayra and optional assistive-context foundations
- Personal Device Test Center and Windows personal-alpha tooling

## Current development priority

The active goal is **Mayra Living Companion Personal Alpha V0.1**. Unrelated major feature work is frozen until the stabilization gate is accepted.

1. Preserve the integration head and rollback path.
2. Obtain a reproducible JDK 17 / SDK 35 / Gradle 8.9 build.
3. Pass compile, complete unit tests and Android lint.
4. Assemble and hash the personal-alpha APK.
5. Install on the owner's physical phone.
6. Validate Living Home, voice, Floating Mayra, reminders, agenda, identities, notifications and global stop.
7. Fix real-device failures before broader intelligence expansion.
8. Add encrypted Backup & Restore before personal beta.

## Important platform boundaries

Mayra never guarantees unsupported third-party app automation. Official Android APIs and app-provided integrations are preferred. Accessibility must be explicitly enabled, visible and deterministic. Sensitive actions require protection. Opening a compose screen is never reported as a delivered message, and handing an action to another app is never reported as verified completion.

## Personal alpha build

Use branch `stabilize/living-companion-v0.1` for stabilization work.

```powershell
.\scripts\verify-personal-alpha-source.ps1 -Strict
.\scripts\build-personal-alpha.ps1 -Clean
.\scripts\install-personal-alpha.ps1
```

Build requirements and behaviour:

- Windows with PowerShell;
- Android Studio embedded JDK 17 or another JDK 17;
- Android SDK Platform 35;
- Gradle 8.9, automatically bootstrapped and SHA-256 verified when needed;
- one Gradle worker and bounded memory for the owner's 4 GB PC;
- generated source/environment reports and APK SHA-256;
- physical-device validation through the in-app Personal Device Test Center.

GitHub Actions currently has an external runner/account condition where jobs can fail before Checkout with no steps or logs. A local Windows build is therefore the primary evidence path until runner execution resumes.
