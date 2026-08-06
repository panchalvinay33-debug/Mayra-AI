# Mayra J6 Notification Context — Owner Device Acceptance

Date: 2026-08-06 (Asia/Kolkata)
Device: Motorola Edge 70 Fusion
Android: 16
Package: `ai.mayra.app.owner`
Candidate source: `77513388928fd713e551c3974cc25e164b58dead`
Stable Owner workflow: #40
Stable Owner artifact: `mayra-stable-owner-apk-40`

## CI gate

The exact candidate source completed successfully across the governed checks before device acceptance was recorded:

- Android CI #2513 — success
- Stable Owner Alpha #40 — success after clean retry of a transient Room/KSP schema race
- Owner Alpha Preflight #48 — success
- J1 Assistant Test #622 — success
- J2 Voice Test #518 — success
- J3 Neural TTS Test #340 — success
- J4 Local LLM Test #291 — success
- Project Governance #694 — success

## Proven on physical device

- Existing permanent-owner installation updated directly without uninstall.
- Mayra remained the selected default Home after update.
- Home rendered the `Notifications` context card without blocking launcher/app access.
- Android initially protected Notification Listener access as a restricted setting; after the user explicitly allowed the restricted setting and then explicitly enabled Notification Access, Mayra gained access through the normal Android settings flow.
- With access enabled, Home rendered `3 active · 1 may need attention`.
- Home displayed aggregate counts only. No sender name, notification title, message text, OTP, account content, or other notification body content appeared on the Home surface.
- The existing `Now` context card remained functional at the same time (`Morning · 86% · charging · Online` was observed).
- App drawer remained usable with 81 of 81 launchable apps on the tested device state.
- After Notification Access was disabled again and Home was refreshed, the notification context returned to the explicit not-granted state without crashing or blocking Home.
- Re-enabling access restored aggregate notification context through the same user-controlled Android setting.

## Result

**PASS — J6 notification-context Home slice is device-proven.**

The accepted scope is a permission-bounded Home context surface backed by aggregate metadata. The Home surface itself does not expose raw notification content and does not require AI inference.

## Privacy and trust boundary

- Notification Access remains explicit, revocable, and controlled by Android settings.
- Home consumes the aggregate `NotificationContextSnapshot`; it does not consume notification title/body/sender/OTP fields.
- Access disabled is represented explicitly as `NotGranted`; stale stored counts are not displayed as if access were still granted.
- Aggregate context is independent of cloud/local AI startup and therefore cannot block the launcher critical path.
- Android's Notification Listener permission is capability-wide at the platform level; the Home contract intentionally exposes only aggregate context.

## Follow-up

Future notification intelligence work must preserve the Home aggregate-only boundary and must not silently expand notification content exposure. Any broader notification-content processing remains a separate opt-in/privacy review and is not approved by this device acceptance.
