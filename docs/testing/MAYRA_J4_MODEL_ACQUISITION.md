# Mayra AI — J4 Model Acquisition Record

Status: **FIRST CANDIDATE IDENTIFIED — OWNER LICENSE ACCEPTANCE / DOWNLOAD REQUIRED**
Date: 2026-08-04

## Candidate

Repository: `litert-community/Gemma3-1B-IT`
Exact LiteRT-LM filename discovered from the repository model link: `gemma3-1b-it-int4.litertlm`
Base model: `google/Gemma-3-1B-IT`
Repository license metadata: `gemma`
Runtime: LiteRT-LM
Role: first J4 runtime compatibility candidate only; not the final Mayra brain.

## Access boundary

The model repository is gated by the Gemma terms. The owner must review/accept the Gemma license on the hosting service before the model file can be downloaded. Mayra must not embed a Hugging Face access token, scrape around the gate, or silently download the model before owner consent.

The J4 engineering package therefore uses Android's document picker: after the owner has lawfully downloaded the exact file, J4 imports it into app-private storage and computes SHA-256 locally.

## Why this exact candidate

The upstream Gemma3-1B-IT model card includes LiteRT-LM Android benchmark results for an int4-QAT candidate around 529 MB, with roughly 1 GB CPU RSS on a Samsung S24 Ultra reference benchmark. Those numbers are reference evidence only; Mayra must measure the Motorola Edge 70 Fusion independently.

The exact Motorola result may be slower/faster or use different RAM. No performance claim is promoted from another device.

## Required owner-side download record

Before J4 Round A, record:

- downloaded filename: `gemma3-1b-it-int4.litertlm`;
- downloaded file size in bytes;
- hosting repository/revision shown at download time;
- confirmation that Gemma terms were reviewed/accepted;
- optional source-provided checksum if the host exposes one.

J4 will provide the authoritative local imported SHA-256 after import and a second independent re-verification.

## Prohibited shortcuts

- do not rename a `.task`, `.tflite`, ZIP or unrelated file to `.litertlm`;
- do not bypass the model access/license gate;
- do not commit the model binary into GitHub;
- do not commit Hugging Face cookies/tokens/credentials;
- do not inflate the normal Mayra APK with this model;
- do not treat a successful import as proof that inference works.

## Promotion path

1. owner obtains exact model after license acceptance;
2. J4 Round A imports/re-verifies/removes/re-imports it;
3. model bytes + SHA-256 + Motorola storage/RAM evidence are recorded;
4. exact LiteRT-LM Android SDK is pinned from CI provenance;
5. a later J4 artifact performs CPU engine initialization;
6. only after device runtime + language quality passes may this model be considered for Mayra integration.
