# Mayra Personal Workspace + Background Assistant — Technical Status

**Status:** Locked feature expansion and honest implementation boundary  
**Target:** Personal owner APK first; public distribution constraints are a later profile  
**Branch:** `stabilize/living-companion-v0.1`

## 1. Product decision

Mayra gains a first-class **Personal Workspace + Background Assistant System**. This extends the current Living Companion architecture; it does not replace or restart the existing chat, voice, reminder, notification, identity, action-safety, encrypted-backup, floating-presence or provenance work.

The primary interaction remains the unified Mayra + Chat screen. Workspace opens when a request needs visible structured work: file evidence, table editing, document preview, export, email preparation, call controls, confirmations or long-running task progress.

## 2. Honest current audit

### Implemented and reusable

- Hindi/Hinglish/English text and Android speech input/output foundation.
- Chat-first launcher and unified companion/chat surface.
- Local deterministic command handling with optional online AI fallback.
- Android Keystore encrypted provider secret pattern.
- Room, WorkManager and Compose dependencies.
- Contacts/relationship identity store and conservative ambiguity handling.
- Review-first dialer and message-composer handoffs.
- Reminder engine with reboot/update recovery and stale-worker protection.
- Notification listener with privacy classification and bounded reply controls.
- Floating foreground-service overlay foundation.
- Optional deterministic Accessibility assist service.
- Persistent Global Stop, startup diagnostics and exact-source artifact verification.
- Existing document picker/reader and explicit encrypted `.mayrabackup` foundation.

### Partially implemented

- Background runtime exists, but not yet a general durable workspace/job orchestrator.
- Document opening exists, but no full inventory, local search index, OCR pipeline or bill schema.
- Memory exists, but not a source-attributed file/document knowledge index.
- Calls open Android dialer; Mayra is not yet a default dialer/InCallService.
- Messaging prepares supported handoffs; WhatsApp has no dedicated recipient-preview bridge yet.
- Android TTS speaks replies, but lock-screen command policy and wake-word entry are not complete.
- Room is configured, but Workspace/File Intelligence entities and migrations are absent.
- Encrypted backup currently covers selected memory data, not workspace sessions, indexes or generated artifacts.

### Missing

- SAF/MediaStore folder grant journey and persisted URI registry.
- Incremental metadata inventory and deep-index workers.
- PDF parser, OCR adapter, receipt/bill extraction and confidence model.
- Encrypted searchable file index.
- Full Workspace task/session UI and durable orchestration.
- Voice-to-table mutation engine, formula layer and exports.
- DOCX/XLSX/PDF/image report renderers.
- Email draft/attachment/confirmation/send engine.
- Default-dialer role, Telecom/InCallService and custom call UI.
- Dedicated WhatsApp preview/deep-link bridge and optional narrow accessibility adapter.
- Lock-screen privacy modes, background voice entry service and wake-word prototype.

## 3. Architecture merge

```text
Voice / Text / Notification / Share / File request
  -> Mayra input normalizer
  -> Typed intent parser
  -> Entity extraction
  -> Workspace session coordinator
  -> Permission + privacy + Global Stop gates
  -> Memory/file/source retrieval
  -> Tool adapter
  -> Confirmation card when required
  -> Execution
  -> Result verification
  -> Source-attributed workspace output
  -> Voice report + audit event
```

### Typed action categories

`SEARCH_FILE`, `ANALYSE_DOCUMENT`, `CREATE_TABLE`, `UPDATE_TABLE`, `EXPORT_DOCUMENT`, `SEND_EMAIL`, `PREPARE_WHATSAPP`, `PLACE_CALL`, `CONTROL_CALL`, `CREATE_REMINDER`, `CREATE_NOTE`, `OPEN_APP`, `READ_NOTIFICATION`, `SEARCH_CONTACT`.

Every important result must carry status, source references, verification state and a user-facing report. Opening an external compose/dial screen is not equivalent to sending or completing the action.

## 4. Module plan

### Milestone A — Workspace foundation

New/reused modules:

- `workspace/MayraWorkspaceModels.kt` — typed requests, results, task states, source references and session models.
- `workspace/MayraWorkspaceIntentParser.kt` — deterministic first-pass Hindi/Hinglish/English intent routing.
- `workspace/MayraWorkspaceSessionStore.kt` — Android-Keystore encrypted autosave.
- `workspace/MayraWorkspaceViewModel.kt` — task/session state coordinator.
- `workspace/MayraWorkspaceActivity.kt` — transcript, task state, notes/table/source/confirmation surface.
- Existing `AndroidVoiceAssistant`, `MayraSettingsStore`, `MayraGlobalStopStore` and local safety engine remain authoritative.

### Milestone B — File intelligence

- `file/FileGrantRegistry`
- `file/MediaStoreInventorySource`
- `file/SafTreeInventorySource`
- `file/FileInventoryWorker`
- `file/DeepIndexWorker`
- `file/FileIndexEntity`, DAO and Room migration
- `document/PdfTextExtractor`
- `ocr/OcrEngine` adapter
- `document/BillParser`
- source/page/confidence records

Shared storage must use Android-supported MediaStore and Storage Access Framework grants. The personal build may request broad practical access where Android permits it, but cannot bypass scoped storage, private app databases, secure folders, banking controls, PINs or OTP protections.

### Milestone C — Table and export

- structured project/session document
- row/column mutation commands
- undo/redo journal
- formula, total, GST, sort/filter and duplicate checks
- CSV first, then PDF/XLSX/DOCX/image adapters
- preview and source document retained separately

### Milestone D — Email

- recipient resolver
- draft model
- attachment URI grants
- preview and explicit send confirmation
- official email intent/provider adapter first
- verified success only when the selected adapter can prove it

### Milestone E — Calling

- current review-first ACTION_DIAL remains fallback
- role-request exploration
- `InCallService` and call-state repository only after default-dialer acceptance
- emergency/system restrictions remain authoritative

### Milestone F — External bridge

- official share/deep-link/intents first
- recipient/message preview
- narrow, deterministic private-build accessibility adapters only where explicitly enabled
- no generic autonomous click engine
- block banking, OTP, password and sensitive-app automation

### Milestone G — Background/lock screen

- visible foreground-service notification with Talk action
- Bluetooth/headset entry exploration
- strict/balanced/trusted/always-authenticate privacy policy
- wake-word research/prototype after battery and privacy measurement
- default assistant/VoiceInteractionService exploration

## 5. Permissions and manifest changes

### Already declared

Internet, camera, microphone, contacts, notifications, exact alarm, boot, biometric, overlay and foreground-service access.

### Required later and only when the corresponding vertical slice exists

- media permissions appropriate to Android version (`READ_MEDIA_IMAGES` and selected document access where applicable);
- persisted SAF tree/document grants instead of pretending unrestricted paths;
- foreground service types that match actual background work;
- default dialer role components and `InCallService` binding permission;
- optional assistant/voice-interaction service declarations;
- email/share URI `FileProvider` only if generated files require it.

No direct CALL_PHONE or SEND_SMS permission is added merely for convenience while the architecture remains review-first.

## 6. Dependencies

Current dependencies already support Compose, WorkManager and Room. Expected additions by milestone:

- PDF text extraction library after size/security review;
- on-device OCR adapter, preferably ML Kit text recognition or a replaceable offline engine;
- Apache POI or a smaller XLSX writer after APK/memory evaluation;
- DOCX writer only after low-memory testing;
- PDF renderer/generator selected for deterministic print output;
- no dependency is added until its vertical slice and tests are ready.

## 7. Database and backup changes

Planned Room schema families:

- workspace sessions, revisions and task events;
- file grants and indexed file metadata;
- extracted document text/page sources;
- parsed bill fields and confidence;
- tables, columns, rows, cells and formula definitions;
- generated artifacts and email drafts;
- bounded audit events.

Every migration must export a Room schema. Workspace sessions, file grants metadata, user-created tables and generated-document metadata join explicit encrypted backup in a later backup schema version. Original external files are referenced by URI and are not silently copied into backup.

## 8. Build-breaking risks

- new launcher/unified UI has not yet received a consolidated compile/lint run;
- adding Room entities without a migration will break existing installs;
- PDF/OCR/Office dependencies can exceed low-memory build and APK budgets;
- SAF URIs can expire or lose permission;
- background service declarations must match Android 14/15 foreground-service rules;
- default dialer and assistant roles vary by OEM;
- private-build accessibility workflows are fragile when third-party UI changes;
- export/email flows need correct content URIs and temporary grant flags;
- generated Kotlin/Compose source must remain compatible with Java 17, minSdk 26 and targetSdk 35.

## 9. Testing plan

### Unit

- intent classification and entity extraction;
- workspace state transitions;
- encrypted-session round trip and corruption recovery;
- table edits, undo/redo, totals, GST, duplicate and missing-field handling;
- bill parsing with confidence and source pages;
- recipient ambiguity and confirmation policy;
- Global Stop cancellation.

### Robolectric/integration

- Keystore-backed stores;
- persisted URI registry behavior;
- WorkManager incremental scheduling;
- manifest/service contracts;
- content URI export grants.

### Instrumented/physical

- document/folder grants across reboot and app update;
- large PDF/image/OCR behavior on the owner phone;
- background notification Talk entry;
- screen-off/lock privacy modes;
- Bluetooth/headset behavior;
- email attachment handoff;
- default dialer role and call controls where supported;
- WhatsApp bridge failure detection;
- first end-to-end XYZ bill -> table -> PDF -> email workflow.

## 10. Exact implementation order

1. Workspace typed models, encrypted autosave, task states, activity and menu entry.
2. Workspace parser and deterministic tests.
3. File-grant journey and metadata inventory.
4. Incremental index Room schema + worker.
5. PDF extraction, OCR adapter and source-attributed search.
6. Bill parser and the XYZ bill voice-result slice.
7. Table model/editor, corrections, undo/redo and CSV export.
8. PDF export and professional templates.
9. Email draft, attachment, preview, confirmation and send handoff.
10. Calling role prototype.
11. WhatsApp bridge.
12. Background/lock-screen modes and wake-word research.
13. Expand encrypted backup and physical acceptance tests.

## 11. First acceptance flow

The locked first end-to-end acceptance remains:

`XYZ bill on phone -> voice search -> verified date/rate/total with source -> workspace table -> spoken correction -> professional PDF -> recipient/subject/body/attachment preview -> confirmed email send -> verified voice report`.

Until every step is physically demonstrated, the feature is reported by its actual validation level: coded, source-preflight verified, compile verified, unit-test verified, installed or physical-device verified.