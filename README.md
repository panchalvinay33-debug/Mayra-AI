# Mayra AI

**Personal Android AI companion / mobile Jarvis project** built with Kotlin and Jetpack Compose.

## Start here

➡️ **Read [`START_HERE.md`](START_HERE.md) before making or reviewing any change.**

It contains the current project truth, document map, implemented capabilities, remaining work, resume procedure and mandatory completion checklist.

## Canonical records

- [`docs/MAYRA_BLUEPRINT.md`](docs/MAYRA_BLUEPRINT.md) — product and architecture blueprint
- [`docs/MAYRA_ROADMAP.md`](docs/MAYRA_ROADMAP.md) — current execution status and ordered next gates
- [`docs/backups/MAYRA_LATEST_SNAPSHOT.md`](docs/backups/MAYRA_LATEST_SNAPSHOT.md) — exact rolling recovery state
- [`docs/MAYRA_IDEA_LEDGER.md`](docs/MAYRA_IDEA_LEDGER.md) — accepted, changed, deferred and removed ideas
- [`docs/MAYRA_DECISIONS.md`](docs/MAYRA_DECISIONS.md) — architecture/product decisions and supersessions
- [`docs/MAYRA_CHANGELOG.md`](docs/MAYRA_CHANGELOG.md) — milestone history
- [`docs/MAYRA_FULL_APP_ACCEPTANCE.md`](docs/MAYRA_FULL_APP_ACCEPTANCE.md) — Motorola physical acceptance checklist
- [`docs/BLUEPRINT_UPDATE_POLICY.md`](docs/BLUEPRINT_UPDATE_POLICY.md) — mandatory governance rules

## Current phase

Mayra 0.2.1 has a CI-verified Personal Alpha foundation covering conversation/provider, memory, documents, voice, reminders, app opening, review-first phone actions and production release auditing. The active phase adds Android Assistant-role, animated voice-session and later local-brain/call-control capabilities.

## Build baseline

- JDK 17
- Android SDK 35
- Gradle 8.9 in CI
- Main application ID: `ai.mayra.app`
- Owner Personal Alpha ID: `ai.mayra.app.alpha`

Do not commit API keys, keystores, passwords, tokens or owner-private data.

## Governance

GitHub Actions runs both Android CI and Project Governance checks. Meaningful implementation work must update the roadmap and rolling snapshot; architecture, idea and release-sensitive changes must update their corresponding canonical records.

PR #12 remains Draft and unmerged until explicit owner approval.
