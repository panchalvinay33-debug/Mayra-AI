# Mayra AI — J5 Launcher Motorola Acceptance

Date created: 2026-08-05
Status: **DEVICE VERIFY IN PROGRESS — DEFAULT HOME + SEARCH/LAUNCH/HOME RETURN PROVEN**
Target device: Motorola Edge 70 Fusion / Android 16

## Exact artifact identity

- source commit: `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a`
- Android workflow/run: Android CI #2384 (`30978034598`)
- package: `ai.mayra.app.alpha`
- version/versionCode: `0.2.1-alpha` / `4`
- Personal Alpha artifact ID: `8919388343`
- artifact ZIP digest: `sha256:92a6aa72d54e48c9fbc835e277b5d3471ec4c9112dfa9ab6e65a16ff229a2e17`
- APK SHA-256: `1ec6be33cb0c484552668145c48690094df3e44a0cb0cef613e28c1f88283096`
- signing/certificate provenance: verify on installed owner artifact before promotion; CI may use owner signing when configured, otherwise debug fallback.
- exact-green backup: `backup/j5-home-contract-ci-green-2026-08-05`

Automated evidence on this exact source: Android #2384, J1 #493, J2 #389, J3 #211, J4 #162 and Governance #565 all SUCCESS. The merged Personal Alpha and Release APK audits explicitly require `MayraLauncherActivity` plus HOME/DEFAULT categories while preserving one normal app LAUNCHER entry.

## Motorola evidence recorded 2026-08-05

Owner-supplied screenshots prove:

1. Android Default apps shows `Home app -> Mayra AI Personal Alpha`.
2. Pressing/returning Home renders `Mayra Home` with `Default Home ✓`.
3. The Home surface enumerates `81 of 81 launchable apps` on the device.
4. The visible app list renders installed apps correctly.
5. Searching `chro` filters the app list to matching results including `Chrome / com.android.chrome`.
6. Tapping Chrome launches the real Chrome app successfully.
7. Pressing Home after Chrome returns to Mayra Home with the search state still visible.
8. `Ask Mayra`, `Refresh apps`, `Switch / restore Home app`, app search field and app list are visible on the real device.

This now proves the core daily launcher loop: select Mayra as Home -> render Home -> search app -> launch app -> return Home. Repeated 20/20 Home returns, lock/reboot, switch-back, failure-independence and resource behavior remain pending.

## Installation precheck

- [x] Download exact artifact `8919388343` / provided extracted APK.
- [x] APK SHA-256 bound to the test artifact in project records.
- [ ] Record installed certificate/signing identity.
- [x] Current Motorola launcher remains installed and available as rollback.
- [x] Mayra data was not deleted merely to change Home app.

## A. Default Home selection

- [x] Mayra appears in Android Home app/default launcher list.
- [x] Owner can select Mayra without error.
- [ ] Home gesture/button returns to Mayra 20/20 times. Multiple successful returns are proven; formal 20/20 count pending.
- [ ] Lock -> unlock returns to a usable Home state.
- [ ] Reboot retains/recovers expected default Home behavior.

## B. Basic launcher usability

- [x] Installed launchable apps are present; first device render reports 81/81 launchable apps.
- [ ] Search finds at least five known apps using label text. Chrome label search is proven; four more known-app label searches remain.
- [ ] Package-name search works for a known installed app where practical.
- [x] Tapping a result launches the correct app; Chrome physically proven.
- [x] Returning Home returns to Mayra after launching another app; Chrome -> Home physically proven.
- [ ] Refresh apps does not create duplicate/ghost entries.
- [ ] App install/update/uninstall is reflected after refresh where tested.

Favorites/basic persistent layout is not implemented in the current slice and is therefore not falsely required for this first proof.

## C. AI failure independence

- [ ] Local model unavailable: Home/app list/search/launch still usable.
- [ ] Local-brain process killed: Home/app list/search/launch still usable.
- [ ] Provider/network unavailable: Home remains usable.
- [ ] Airplane mode: Home remains usable.
- [ ] Neural TTS unavailable: Home remains usable.
- [ ] Notification Access denied: Home remains usable.
- [ ] Contacts denied: Home remains usable.

## D. Assistant coexistence

- [ ] `Ask Mayra` opens the normal Mayra activity.
- [ ] Power-button Digital Assistant invocation still works where configured.
- [ ] Dismissing Assistant returns to a sane screen/Home.
- [ ] No Assistant <-> Launcher navigation loop.
- [ ] Rapid invoke/dismiss/Home cycle remains stable.

Deeper voice/orb integration into Home is intentionally a later J5 slice after base Home reliability proof.

## E. Recovery / switch-back

- [ ] `Switch / restore Home app` opens Android Home settings.
- [ ] Previous launcher can be selected again.
- [ ] Switching away does not delete Mayra data.
- [ ] Switching back to Mayra works.
- [ ] Force-stop/restart does not trap the owner.
- [ ] A launcher activity/process failure recovers to a selectable usable Home path.

## F. Resource behavior

Record:
- launcher idle RAM:
- launcher active RAM:
- local-brain separate-process RAM:
- 30-minute idle battery observation:
- temperature/thermal notes:
- visible jank/ANR/crash count:

## G. Result recording

For every failed item record exact step, visible error, screenshot/log if useful, and whether switching back to the previous launcher still works. Never convert a failed checkbox to pass by weakening the requirement.

## Promotion rule

J5 remains `DEVICE_VERIFY` until required items above are physically proven on the exact artifact/source. Any trap, Home loop, inability to restore another launcher, material data loss, crash loop, or basic app-access failure blocks promotion.

After pass:
1. synchronize Roadmap/Blueprint/Idea/Decision/Changelog/Latest Snapshot;
2. record exact device/build/signing evidence;
3. create immutable J5 milestone snapshot;
4. create protected exact-green J5 baseline branch;
5. only then begin J6 context-card integration.