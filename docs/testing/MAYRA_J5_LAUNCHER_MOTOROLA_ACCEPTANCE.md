# Mayra AI — J5 Launcher Motorola Acceptance

Date created: 2026-08-05
Status: NOT RUN — implementation blocked until J4 exact-head green
Target device: Motorola Edge 70 Fusion / Android 16

## Artifact identity

Before installation record:

- source commit:
- workflow/run:
- package:
- version/versionCode:
- APK artifact ID:
- APK SHA-256:
- signing/certificate provenance:

## A. Default Home selection

- [ ] Mayra appears in Android Home app/default launcher list.
- [ ] Owner can select Mayra without error.
- [ ] Home gesture/button returns to Mayra 20/20 times.
- [ ] Lock → unlock returns to a usable Home state.
- [ ] Reboot retains/recovers expected default Home behavior.

## B. Basic launcher usability

- [ ] Installed launchable apps are present.
- [ ] App search finds common installed apps.
- [ ] Tapping a result launches the correct app.
- [ ] Returning Home returns to Mayra.
- [ ] Favorites/basic layout persists across restart.
- [ ] No duplicate/ghost app entries after install/update where tested.

## C. AI failure independence

- [ ] Local model unavailable: Home/app drawer still usable.
- [ ] Local model process killed: Home/app drawer still usable.
- [ ] Provider/network unavailable: Home/app drawer still usable.
- [ ] Neural TTS unavailable: Home/app drawer still usable.
- [ ] Notification Access denied: Home degrades cleanly.
- [ ] Contacts denied: Home degrades cleanly.

## D. Assistant coexistence

- [ ] Mayra voice/orb entry from Home works.
- [ ] Power-button Digital Assistant invocation still works where configured.
- [ ] Dismissing Assistant returns to a sane screen/Home.
- [ ] No Assistant ↔ Launcher navigation loop.
- [ ] Rapid invoke/dismiss/Home cycle remains stable.

## E. Recovery / switch-back

- [ ] Owner can open Android Home app settings.
- [ ] Previous launcher can be selected again.
- [ ] Switching away does not delete Mayra data.
- [ ] Switching back to Mayra works.
- [ ] A launcher crash/restart does not trap the owner.

## F. Resource behavior

Record idle and active observations:

- launcher idle RAM:
- launcher active RAM:
- local-brain separate process RAM:
- 30-minute idle battery observation:
- temperature/thermal notes:
- visible jank/ANR/crash count:

## Promotion rule

J5 remains `DEVICE_VERIFY` or lower until every required item above is proven on the exact promoted source. Any failure is recorded; it is not hidden by changing the checklist.

After pass:

1. synchronize Roadmap/Blueprint/Idea/Decision/Changelog/Latest Snapshot;
2. record artifact/source/device evidence;
3. create immutable J5 milestone snapshot;
4. create protected exact-green J5 baseline branch;
5. only then begin J6 context-card integration.
