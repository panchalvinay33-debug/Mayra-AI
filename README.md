# Mayra AI

Intelligent personal AI assistant project for Android, built with Kotlin and Jetpack Compose.

## Phase 1 foundation

- Android app structure and Compose UI
- Stateful local chat flow using ViewModel + StateFlow
- Replaceable AI service boundary (`MayraAssistant`)
- Android speech-to-text foundation
- Android text-to-speech foundation
- Runtime microphone permission flow
- Voice transcript wired into chat input
- Low-memory Gradle settings for development
- Secret/local file protection

## Current limitation

The included `LocalMayraAssistant` is intentionally a development placeholder. A production AI backend is not yet connected and no API secrets are stored in this repository.

## First build

1. Clone/open this repository in Android Studio.
2. Use JDK 17 and Android SDK 35.
3. Allow Gradle sync to finish.
4. Run a debug build on a physical Android device where possible.
5. Grant microphone permission when testing voice input.

Actual device/build validation is required before Phase 1 is considered fully verified.
