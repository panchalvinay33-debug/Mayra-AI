# Mayra Motorola Installation Result — Play Protect Block

Date: 2026-08-03
Device: Motorola owner device, Android 16
Attempted package: `ai.mayra.app.alpha`
Attempted artifact: Personal Alpha #1851
Source baseline: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
APK SHA-256: `1459517f1aa375576afa353ba6683ceaf81ddbcb4e79fc6dd790a501f52307b8`

## Result

**BLOCKED**

Google Play Protect displayed:

> App blocked to protect your device
>
> This app can request access to sensitive data. This can increase the risk of identity theft or financial fraud.

The app was not installed.

## Interpretation

This is a distribution/trust-channel failure, not proof that the source is malicious and not proof that the APK is safe enough to bypass the warning. The full debug-signed Personal Alpha declares several sensitive capabilities and was delivered by sideload rather than a trusted owner-signing/Play channel.

## Owner instruction

Do not disable or bypass Play Protect for this artifact. Do not keep retrying the same full Personal Alpha APK.

## Corrective architecture

### J1 system-assistant proof

Use a dedicated zero-permission package `ai.mayra.app.j1` containing only:

- one small Assistant activation/status activity;
- VoiceInteractionService;
- VoiceInteractionSessionService and animated orb;
- RecognitionService metadata shell.

It must contain no requested Android permissions, internet, contacts, notifications, boot receiver, notification listener, provider, reminders, documents, memory or full chat runtime.

### Full owner Mayra

Distribute the full app through a stable private certificate and preferably Google Play Internal Testing. Install-over-install updates must preserve local owner data. Temporary GitHub runner debug certificates are not an acceptable long-term owner distribution mechanism.

## Required retest

1. J1 zero-permission APK installs without Play Protect block.
2. Package/label/one launcher verified.
3. Mayra appears in Assistant role selection.
4. Role selection and removal work.
5. Unlocked/locked invocation and orb session are tested.
6. Full owner app is later tested through the stable signed/trusted distribution path.

Status remains BLOCKED until new artifact evidence is recorded.
