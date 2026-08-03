#!/usr/bin/env bash
set -euo pipefail

BASE_REF="${1:-}"
HEAD_REF="${2:-HEAD}"

required_files=(
  "START_HERE.md"
  "README.md"
  "docs/MAYRA_BLUEPRINT.md"
  "docs/MAYRA_ROADMAP.md"
  "docs/backups/MAYRA_LATEST_SNAPSHOT.md"
  "docs/MAYRA_IDEA_LEDGER.md"
  "docs/MAYRA_DECISIONS.md"
  "docs/MAYRA_CHANGELOG.md"
  "docs/MAYRA_FULL_APP_ACCEPTANCE.md"
  "docs/BLUEPRINT_UPDATE_POLICY.md"
)

fail() {
  echo "Mayra governance check failed: $*" >&2
  exit 1
}

for file in "${required_files[@]}"; do
  [[ -s "$file" ]] || fail "required project record is missing or empty: $file"
done

# START_HERE is deliberately validated as the stable project entry point.
grep -Fq "# Mayra AI — START HERE" START_HERE.md || fail "START_HERE.md has no canonical title"
grep -Fq "## 5. Mandatory resume procedure" START_HERE.md || fail "START_HERE.md has no resume procedure"
grep -Fq "docs/MAYRA_BLUEPRINT.md" START_HERE.md || fail "START_HERE.md does not link the blueprint"
grep -Fq "docs/MAYRA_ROADMAP.md" START_HERE.md || fail "START_HERE.md does not link the roadmap"
grep -Fq "docs/backups/MAYRA_LATEST_SNAPSHOT.md" START_HERE.md || fail "START_HERE.md does not link the rolling snapshot"

grep -Fq "## Product vision" docs/MAYRA_BLUEPRINT.md || fail "blueprint has no product vision"
grep -Fq "## Overall program view" docs/MAYRA_ROADMAP.md || fail "roadmap has no overall program view"
grep -Fq "## Active ideas" docs/MAYRA_IDEA_LEDGER.md || fail "idea ledger has no active ideas section"
grep -Fq "## ADR-" docs/MAYRA_DECISIONS.md || fail "decision log has no ADR entries"

# Ensure no secrets or private signing materials were accidentally placed in governance records.
if grep -Eir --include='*.md' \
  '(sk-[A-Za-z0-9_-]{20,}|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|MAYRA_RELEASE_STORE_PASSWORD=|MAYRA_RELEASE_KEY_PASSWORD=)' \
  START_HERE.md docs; then
  fail "possible credential or private key material found in project records"
fi

if [[ -z "$BASE_REF" ]]; then
  echo "No base ref supplied; structural governance checks passed."
  exit 0
fi

if ! git rev-parse --verify "$BASE_REF" >/dev/null 2>&1; then
  fail "base ref cannot be resolved: $BASE_REF"
fi
if ! git rev-parse --verify "$HEAD_REF" >/dev/null 2>&1; then
  fail "head ref cannot be resolved: $HEAD_REF"
fi

mapfile -t changed_files < <(git diff --name-only "$BASE_REF...$HEAD_REF")
printf '%s\n' "${changed_files[@]}" > build-governance-changed-files.txt

if [[ ${#changed_files[@]} -eq 0 ]]; then
  echo "No changed files; governance checks passed."
  exit 0
fi

is_changed() {
  local target="$1"
  printf '%s\n' "${changed_files[@]}" | grep -Fxq "$target"
}

meaningful_change=false
architecture_change=false
idea_sensitive_change=false
release_change=false

for file in "${changed_files[@]}"; do
  case "$file" in
    app/*|build.gradle.kts|settings.gradle.kts|gradle/*|.github/workflows/*|scripts/*)
      meaningful_change=true
      ;;
  esac
  case "$file" in
    app/src/main/AndroidManifest.xml|app/build.gradle.kts|app/src/main/java/ai/mayra/app/MayraApplication.kt|app/src/main/java/ai/mayra/app/core/*|app/src/main/java/ai/mayra/app/assistant/*|app/src/main/java/ai/mayra/app/call/*|app/src/main/java/ai/mayra/app/background/*)
      architecture_change=true
      ;;
  esac
  case "$file" in
    app/src/main/java/ai/mayra/app/assistant/*|app/src/main/java/ai/mayra/app/call/*|app/src/main/java/ai/mayra/app/reminder/*|app/src/main/java/ai/mayra/app/memory/*|app/src/main/java/ai/mayra/app/document/*)
      idea_sensitive_change=true
      ;;
  esac
  case "$file" in
    app/build.gradle.kts|app/src/*/AndroidManifest.xml|.github/workflows/*|app/proguard-rules.pro)
      release_change=true
      ;;
  esac
done

if [[ "$meaningful_change" == true ]]; then
  is_changed "docs/MAYRA_ROADMAP.md" || fail "meaningful project changes require docs/MAYRA_ROADMAP.md update"
  is_changed "docs/backups/MAYRA_LATEST_SNAPSHOT.md" || fail "meaningful project changes require docs/backups/MAYRA_LATEST_SNAPSHOT.md update"
fi

if [[ "$architecture_change" == true ]]; then
  is_changed "docs/MAYRA_BLUEPRINT.md" || fail "architecture/background/core changes require docs/MAYRA_BLUEPRINT.md update"
  is_changed "docs/MAYRA_DECISIONS.md" || fail "architecture/background/core changes require docs/MAYRA_DECISIONS.md update"
fi

if [[ "$idea_sensitive_change" == true ]]; then
  is_changed "docs/MAYRA_IDEA_LEDGER.md" || fail "feature-track changes require docs/MAYRA_IDEA_LEDGER.md update"
fi

if [[ "$release_change" == true ]]; then
  is_changed "docs/MAYRA_CHANGELOG.md" || fail "release/build/manifest changes require docs/MAYRA_CHANGELOG.md update"
fi

echo "Mayra project governance checks passed."
