# Mayra AI — Phase 1 Foundation

Mayra AI is an Android assistant project built with Kotlin and Jetpack Compose.

## Implemented in Phase 1 so far
- Android application foundation
- Jetpack Compose UI and theme
- Chat message model and state architecture
- Replaceable `MayraAssistant` AI interface
- Local placeholder assistant for offline development
- Android speech recognition implementation
- Android text-to-speech implementation
- Microphone permission declaration/helper
- Low-memory Gradle configuration for development machines
- Secret/local-file protection through `.gitignore`

## Important
The current local assistant is a development placeholder. No production AI API key or cloud AI provider is embedded in the repository. Secrets must never be committed to GitHub.

## Next validation steps
1. Open the repository as an Android Studio project.
2. Use JDK 17.
3. Sync Gradle and install Android SDK 35 if requested.
4. Build the debug app and resolve any environment-specific dependency issues.
5. Test on a physical Android device, especially microphone permission, speech recognition and TTS availability.

## Planned next layers
- Connect voice controls into the Compose UI with runtime permission request flow.
- Production AI backend integration through a secure server/API boundary.
- Conversation persistence and memory architecture.
- Assistant actions with explicit Android permissions and user controls.
