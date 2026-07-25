param(
    [switch]$Strict
)

$ErrorActionPreference = "Stop"

function Write-Check([string]$Label, [bool]$Passed, [string]$Detail) {
    $status = if ($Passed) { "PASS" } else { "FAIL" }
    $color = if ($Passed) { "Green" } else { "Red" }
    Write-Host ("[{0}] {1} - {2}" -f $status, $Label, $Detail) -ForegroundColor $color
    return [PSCustomObject]@{ Label = $Label; Passed = $Passed; Detail = $Detail }
}

function Read-Text([string]$RelativePath) {
    return Get-Content (Join-Path $repoRoot $RelativePath) -Raw
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot
$results = New-Object System.Collections.Generic.List[object]

$requiredFiles = @(
    "settings.gradle.kts",
    "build.gradle.kts",
    "gradle.properties",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    ".github/workflows/android-ci.yml",
    "app/src/main/java/ai/mayra/app/MayraApplication.kt",
    "app/src/main/java/ai/mayra/app/diagnostics/MayraStartupHealth.kt",
    "app/src/main/java/ai/mayra/app/diagnostics/MayraStartupDiagnosticsActivity.kt",
    "app/src/test/java/ai/mayra/app/diagnostics/MayraStartupHealthTest.kt",
    "app/src/main/java/ai/mayra/app/safety/MayraGlobalStop.kt",
    "app/src/test/java/ai/mayra/app/safety/MayraGlobalStopStoreTest.kt",
    "app/src/main/java/ai/mayra/app/action/MayraActionRuntime.kt",
    "app/src/main/java/ai/mayra/app/background/MayraBootReceiver.kt",
    "app/src/main/java/ai/mayra/app/background/MayraNotificationListener.kt",
    "app/src/main/java/ai/mayra/app/background/MayraNotificationSafetyPolicy.kt",
    "app/src/test/java/ai/mayra/app/background/MayraNotificationSafetyPolicyTest.kt",
    "app/src/main/java/ai/mayra/app/reminder/MayraReminderEngine.kt",
    "app/src/main/java/ai/mayra/app/reminder/MayraReminderRuntime.kt",
    "app/src/main/java/ai/mayra/app/reminder/ReminderRecoveryPolicy.kt",
    "app/src/test/java/ai/mayra/app/reminder/MayraReminderEngineTest.kt",
    "app/src/test/java/ai/mayra/app/reminder/ReminderRecoveryPolicyTest.kt",
    "app/src/main/java/ai/mayra/app/ai/AiProviderSettings.kt",
    "app/src/main/java/ai/mayra/app/ai/AiProviderSafetyPolicy.kt",
    "app/src/main/java/ai/mayra/app/ai/OpenAiAssistant.kt",
    "app/src/test/java/ai/mayra/app/ai/AiProviderSafetyPolicyTest.kt",
    "app/src/main/java/ai/mayra/app/identity/MayraContactIdentity.kt",
    "app/src/test/java/ai/mayra/app/identity/MayraContactIdentityEngineTest.kt",
    "app/src/main/java/ai/mayra/app/core/actions/DeviceActionModels.kt",
    "app/src/main/java/ai/mayra/app/core/actions/AndroidDeviceActionSpec.kt",
    "app/src/test/java/ai/mayra/app/core/actions/DeviceActionSafetyGateReliabilityTest.kt",
    "app/src/main/java/ai/mayra/app/memory/MayraMemoryBackupEngine.kt",
    "app/src/test/java/ai/mayra/app/memory/MayraMemoryBackupEngineTest.kt",
    "app/src/main/java/ai/mayra/app/testing/MayraDeviceTestCenter.kt",
    "app/src/test/java/ai/mayra/app/testing/MayraDeviceTestCenterTest.kt",
    "docs/MAYRA_AI_MASTER_BLUEPRINT.md",
    "docs/MAYRA_LIVING_INTELLIGENCE_VISION.md",
    "docs/MAYRA_SOURCE_OF_TRUTH_AND_BACKUP_MAP.md",
    "docs/PERSONAL_ALPHA_STABILIZATION_STATUS.md",
    "docs/AI_PROVIDER_SECURITY_STATUS.md",
    "docs/CONTACT_AND_ACTION_HANDOFF_SAFETY_STATUS.md",
    "scripts/build-personal-alpha.ps1",
    "scripts/install-personal-alpha.ps1",
    "scripts/verify-personal-alpha-artifact.ps1",
    "scripts/test-artifact-provenance.ps1"
)

foreach ($path in $requiredFiles) {
    $results.Add((Write-Check "Required file" (Test-Path (Join-Path $repoRoot $path)) $path))
}

$settingsText = Read-Text "settings.gradle.kts"
$appGradle = Read-Text "app/build.gradle.kts"
$properties = Read-Text "gradle.properties"
$manifest = Read-Text "app/src/main/AndroidManifest.xml"
$results.Add((Write-Check "Android app module" ($settingsText -match 'include\(":app"\)') 'settings.gradle.kts includes :app'))
$results.Add((Write-Check "Compile SDK" ($appGradle -match 'compileSdk\s*=\s*35') 'compileSdk 35'))
$results.Add((Write-Check "Target SDK" ($appGradle -match 'targetSdk\s*=\s*35') 'targetSdk 35'))
$results.Add((Write-Check "Java target" ($appGradle -match 'JavaVersion\.VERSION_17') 'Java 17 bytecode'))
$results.Add((Write-Check "Version name" ($appGradle -match 'versionName\s*=\s*"0\.1\.0"') 'Personal Alpha V0.1'))
$results.Add((Write-Check "Bounded Gradle memory" ($properties -match 'org\.gradle\.jvmargs=-Xmx(1024|1280|1536)m') 'Suitable for low-memory Windows build'))
$results.Add((Write-Check "Parallel Gradle disabled" ($properties -match 'org\.gradle\.parallel=false') 'Avoids 4 GB RAM pressure'))

$trackedSecretPatterns = @(
    '(?i)sk-[a-z0-9_-]{20,}',
    '(?i)api[_-]?key\s*[=:]\s*[^\s]{16,}',
    '(?i)client[_-]?secret\s*[=:]\s*[^\s]{16,}',
    '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----'
)
$scanExtensions = @("*.kt", "*.kts", "*.java", "*.xml", "*.json", "*.md", "*.yml", "*.yaml", "*.properties", "*.ps1")
$secretHits = New-Object System.Collections.Generic.List[string]
foreach ($extension in $scanExtensions) {
    Get-ChildItem -Path $repoRoot -Recurse -File -Filter $extension -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notmatch '[\\/](build|\.gradle|\.git|\.tools)[\\/]' } |
        ForEach-Object {
            $text = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
            foreach ($pattern in $trackedSecretPatterns) {
                if ($text -match $pattern) {
                    $secretHits.Add($_.FullName.Substring($repoRoot.Length + 1))
                    break
                }
            }
        }
}
$noSecrets = $secretHits.Count -eq 0
$secretDetail = if ($noSecrets) { "No obvious tracked secrets found" } else { ($secretHits | Sort-Object -Unique) -join ", " }
$results.Add((Write-Check "Tracked secret scan" $noSecrets $secretDetail))

$results.Add((Write-Check "Launcher declared" ($manifest -match 'android\.intent\.category\.LAUNCHER') 'Launcher entry exists'))
$results.Add((Write-Check "Internet permission" ($manifest -match 'android\.permission\.INTERNET') 'Optional online AI networking declared'))
$results.Add((Write-Check "Notification permission" ($manifest -match 'android\.permission\.POST_NOTIFICATIONS') 'Android 13+ alerts declared'))
$results.Add((Write-Check "Direct call permission absent" ($manifest -notmatch 'android\.permission\.CALL_PHONE') 'Calls remain user-reviewed ACTION_DIAL handoffs'))
$results.Add((Write-Check "Direct SMS permission absent" ($manifest -notmatch 'android\.permission\.SEND_SMS') 'Messages remain user-reviewed composer handoffs'))
$results.Add((Write-Check "Android auto backup disabled" ($manifest -match 'android:allowBackup="false"') 'Sensitive state uses explicit Mayra backup only'))
$results.Add((Write-Check "Full backup disabled" ($manifest -match 'android:fullBackupContent="false"') 'No implicit cloud extraction'))
$results.Add((Write-Check "Cleartext traffic disabled" ($manifest -match 'android:usesCleartextTraffic="false"') 'Network providers require encryption'))
$results.Add((Write-Check "Startup diagnostics private" ($manifest -match 'MayraStartupDiagnosticsActivity" android:exported="false"') 'Startup health is owner-only'))
$exportedActivities = [regex]::Matches($manifest, '<activity[^>]+android:exported="true"').Count
$results.Add((Write-Check "Only launcher exported" ($exportedActivities -eq 1 -and $manifest -match 'MayraPresenceActivity" android:exported="true"') "Found $exportedActivities exported activity"))

$applicationText = Read-Text "app/src/main/java/ai/mayra/app/MayraApplication.kt"
$results.Add((Write-Check "Startup health begins" ($applicationText -match 'startupHealth\.begin\(\)') 'Interrupted starts can be detected'))
$results.Add((Write-Check "Startup health completes" ($applicationText -match 'startupHealth\.complete\(\)') 'Successful starts are marked complete'))
$safeStartupSteps = [regex]::Matches($applicationText, 'startupHealth\.safeStep\(').Count
$results.Add((Write-Check "Non-critical startup containment" ($safeStartupSteps -ge 10) "Found $safeStartupSteps guarded startup steps"))

$globalStopText = Read-Text "app/src/main/java/ai/mayra/app/safety/MayraGlobalStop.kt"
$actionRuntimeText = Read-Text "app/src/main/java/ai/mayra/app/action/MayraActionRuntime.kt"
$bootReceiverText = Read-Text "app/src/main/java/ai/mayra/app/background/MayraBootReceiver.kt"
$results.Add((Write-Check "Persistent Global Stop" ($globalStopText -match 'mayra_global_stop' -and $globalStopText -match 'KEY_GENERATION') 'Stop survives process death and reboot'))
$results.Add((Write-Check "Action runtime obeys Global Stop" ($actionRuntimeText -match 'MayraGlobalStopStore' -and $actionRuntimeText -match 'store\.isStopped\(\)') 'Action engine restores persisted stop state'))
$results.Add((Write-Check "Boot path obeys Global Stop" ($bootReceiverText -match 'MayraGlobalStopStore' -and $bootReceiverText -match 'if \(globallyStopped\)') 'Automation remains stopped after reboot'))
$results.Add((Write-Check "Reminders survive Global Stop" ($bootReceiverText -match 'MayraReminderRuntime\.rescheduleAll' -and $bootReceiverText.IndexOf('MayraReminderRuntime.rescheduleAll') -lt $bootReceiverText.IndexOf('if (globallyStopped)')) 'Owner reminders remain scheduled'))

$notificationListenerText = Read-Text "app/src/main/java/ai/mayra/app/background/MayraNotificationListener.kt"
$notificationSafetyText = Read-Text "app/src/main/java/ai/mayra/app/background/MayraNotificationSafetyPolicy.kt"
$results.Add((Write-Check "Notification pipeline obeys Global Stop" ($notificationListenerText -match 'MayraGlobalStopStore' -and $notificationListenerText -match 'globalStopActive = stopped') 'Reply and proactive actions stop'))
$results.Add((Write-Check "Sensitive notification store-only policy" ($notificationSafetyText -match 'sensitivity == NotificationSensitivity.NORMAL' -and $notificationSafetyText -match 'sensitivity != NotificationSensitivity.OTP') 'OTP and sensitive items are not proactive'))
$results.Add((Write-Check "Conversation identity redaction" ($notificationSafetyText -match 'Protected conversation' -and $notificationSafetyText -match 'Private conversation') 'Sensitive labels are hidden'))

$reminderEngineText = Read-Text "app/src/main/java/ai/mayra/app/reminder/MayraReminderEngine.kt"
$reminderRuntimeText = Read-Text "app/src/main/java/ai/mayra/app/reminder/MayraReminderRuntime.kt"
$reminderRecoveryText = Read-Text "app/src/main/java/ai/mayra/app/reminder/ReminderRecoveryPolicy.kt"
$results.Add((Write-Check "Reminder revision persistence" ($reminderEngineText -match 'val revision: Long' -and $reminderEngineText -match 'put\("revision", revision\)') 'Reminder mutations carry revision'))
$results.Add((Write-Check "Stale reminder worker protection" ($reminderRuntimeText -match 'KEY_REVISION' -and $reminderRuntimeText -match 'reminder\.revision != expectedRevision' -and $reminderRuntimeText -match 'reminder\.dueAt != expectedDueAt') 'Old workers cannot fire changed reminders'))
$results.Add((Write-Check "Stale reminder action protection" ($reminderRuntimeText -match 'EXTRA_REVISION' -and $reminderRuntimeText -match 'current\.revision != expectedRevision') 'Old buttons cannot mutate newer reminders'))
$results.Add((Write-Check "Reminder reboot recovery policy" ($reminderRecoveryText -match 'LEAVE_MISSED' -and $reminderRecoveryText -match 'MARK_MISSED_AND_NOTIFY') 'Already-missed reminders remain quiet'))
$results.Add((Write-Check "Terminal reminder protection" ($reminderEngineText -match 'canComplete' -and $reminderEngineText -match 'canSnooze' -and $reminderEngineText -match 'return@update null') 'Terminal reminders cannot revive'))

$providerSettingsText = Read-Text "app/src/main/java/ai/mayra/app/ai/AiProviderSettings.kt"
$providerSafetyText = Read-Text "app/src/main/java/ai/mayra/app/ai/AiProviderSafetyPolicy.kt"
$onlineAssistantText = Read-Text "app/src/main/java/ai/mayra/app/ai/OpenAiAssistant.kt"
$results.Add((Write-Check "API key encrypted with Android Keystore" ($providerSettingsText -match 'AndroidKeyStore' -and $providerSettingsText -match 'AES/GCM/NoPadding') 'Provider key is encrypted'))
$results.Add((Write-Check "Corrupted provider secret recovery" ($providerSettingsText -match 'hasReadableSecret' -and $providerSettingsText -match 'clearCorrupted') 'Unreadable encrypted keys are removed'))
$results.Add((Write-Check "Provider diagnostic secret redaction" ($providerSafetyText -match 'Bearer \[redacted\]' -and $providerSafetyText -match '\[redacted-key\]') 'Connection messages cannot persist key material'))
$results.Add((Write-Check "Online AI HTTPS enforcement" ($onlineAssistantText -match 'requireHttpsEndpoint' -and $onlineAssistantText -match 'instanceFollowRedirects = false') 'Provider cannot downgrade or redirect silently'))
$results.Add((Write-Check "Online AI bounded transport" ($onlineAssistantText -match 'MAX_REQUEST_CHARACTERS' -and $onlineAssistantText -match 'MAX_RESPONSE_CHARACTERS' -and $onlineAssistantText -match 'readAtMost') 'Large transport is bounded'))
$results.Add((Write-Check "Offline AI fallback retained" ($onlineAssistantText -match 'offlineFallback' -and $onlineAssistantText -match 'offline brain') 'Remote failure retains local Mayra'))

$identityText = Read-Text "app/src/main/java/ai/mayra/app/identity/MayraContactIdentity.kt"
$actionModelsText = Read-Text "app/src/main/java/ai/mayra/app/core/actions/DeviceActionModels.kt"
$actionSpecText = Read-Text "app/src/main/java/ai/mayra/app/core/actions/AndroidDeviceActionSpec.kt"
$safeExecutorText = Read-Text "app/src/main/java/ai/mayra/app/platform/device/MayraSafeActionExecutor.kt"
$androidExecutorText = Read-Text "app/src/main/java/ai/mayra/app/platform/device/AndroidActionExecutor.kt"
$results.Add((Write-Check "Conservative identity matching" ($identityText -match 'MIN_PARTIAL_QUERY_LENGTH' -and $identityText -match 'Ambiguous') 'Short and tied contact matches are not guessed'))
$results.Add((Write-Check "Action fingerprint duplicate protection" ($actionModelsText -match 'safetyFingerprint' -and $actionModelsText -match 'DUPLICATE_BLOCKED' -and $actionModelsText -match 'MAX_REQUEST_AGE_MILLIS') 'Duplicate and stale actions are blocked'))
$results.Add((Write-Check "Review-first call handoff" ($actionSpecText -match 'ACTION_DIAL' -and $actionSpecText -notmatch 'ACTION_CALL') 'Owner starts the final call'))
$results.Add((Write-Check "Review-first message handoff" ($actionSpecText -match 'ACTION_SENDTO' -and $actionSpecText -match 'smsto:') 'Owner sends the final message'))
$results.Add((Write-Check "Owner identity integrated" ($safeExecutorText -match 'MayraContactIdentityStore' -and $androidExecutorText -match 'MayraContactIdentityStore') 'Both production executors use owner identity mapping'))
$results.Add((Write-Check "Single pending confirmation" ($safeExecutorText -match 'Finish or cancel the pending action first' -and $androidExecutorText -match 'Finish or cancel the pending action first') 'Executors cannot orphan confirmation tickets'))

$backupText = Read-Text "app/src/main/java/ai/mayra/app/memory/MayraMemoryBackupEngine.kt"
$results.Add((Write-Check "Authenticated backup encryption" ($backupText -match 'AES/GCM/NoPadding') 'Backup uses AES-GCM'))
$results.Add((Write-Check "Password key derivation" ($backupText -match 'PBKDF2WithHmacSHA256') 'Backup key uses PBKDF2-HMAC-SHA256'))
$results.Add((Write-Check "Versioned backup envelope" ($backupText -match 'MAYRA_ENCRYPTED_BACKUP_V1') 'Unsupported formats can be rejected'))

$buildScriptText = Read-Text "scripts/build-personal-alpha.ps1"
$installScriptText = Read-Text "scripts/install-personal-alpha.ps1"
$artifactVerifierText = Read-Text "scripts/verify-personal-alpha-artifact.ps1"
$artifactTestText = Read-Text "scripts/test-artifact-provenance.ps1"
$ciText = Read-Text ".github/workflows/android-ci.yml"
$results.Add((Write-Check "Build invokes strict preflight" ($buildScriptText -match 'verify-personal-alpha-source\.ps1' -and $buildScriptText -match '-Strict') 'Every controlled local build checks source'))
$results.Add((Write-Check "Build requires clean exact source" ($buildScriptText -match 'git status' -and $buildScriptText -match 'uncommitted changes' -and $buildScriptText -match '^[\s\S]*artifact-manifest\.json') 'Artifact records one clean commit'))
$results.Add((Write-Check "Artifact manifest schema" ($buildScriptText -match 'mayra\.personal-alpha\.artifact\.v1' -and $artifactVerifierText -match 'mayra\.personal-alpha\.artifact\.v1') 'Builder and verifier share schema'))
$results.Add((Write-Check "Artifact hash and size verification" ($artifactVerifierText -match 'Get-FileHash' -and $artifactVerifierText -match 'size mismatch') 'Tampered or truncated APK is rejected'))
$results.Add((Write-Check "Installer verifies provenance" ($installScriptText -match 'verify-personal-alpha-artifact\.ps1' -and $installScriptText -match 'AllowUnverifiedArtifact') 'Unverified installs require explicit diagnostic override'))
$results.Add((Write-Check "Installer grants no direct action permissions" ($installScriptText -notmatch 'android\.permission\.CALL_PHONE' -and $installScriptText -notmatch 'android\.permission\.SEND_SMS') 'Installer matches review-first manifest'))
$results.Add((Write-Check "Artifact tamper regression tests" ($artifactTestText -match 'Tampered APK must fail' -and $artifactTestText -match 'Wrong hash must fail') 'Provenance controls have negative tests'))
$results.Add((Write-Check "CI source preflight" ($ciText -match 'Strict source preflight' -and $ciText -match 'verify-personal-alpha-source\.ps1') 'CI checks source before Gradle'))
$results.Add((Write-Check "CI complete gate chain" ($ciText -match 'compileDebugKotlin' -and $ciText -match 'testDebugUnitTest' -and $ciText -match 'lintDebug' -and $ciText -match 'assembleDebug') 'CI proves compile, tests, lint and APK'))
$results.Add((Write-Check "CI verifies generated artifact" ($ciText -match 'Verify generated artifact manifest' -and $ciText -match 'verify-personal-alpha-artifact\.ps1') 'Uploaded APK is reverified'))
$results.Add((Write-Check "CI checkout cannot persist credentials" ($ciText -match 'persist-credentials: false') 'Build steps cannot reuse checkout credentials'))

$deviceTestText = Read-Text "app/src/main/java/ai/mayra/app/testing/MayraDeviceTestCenter.kt"
$deviceIds = [regex]::Matches($deviceTestText, 'DeviceTestDefinition\(DeviceTestId\.')
$results.Add((Write-Check "Twenty physical alpha checks" ($deviceIds.Count -eq 20) "Found $($deviceIds.Count) DeviceTestDefinition entries"))
$results.Add((Write-Check "Floating Mayra acceptance" ($deviceTestText -match 'DeviceTestId\.FLOATING_MAYRA') 'Overlay lifecycle is physically tested'))
$results.Add((Write-Check "Backup restore acceptance" ($deviceTestText -match 'DeviceTestId\.MEMORY_BACKUP_RESTORE') 'Encrypted restore is physically tested'))

$gitAvailable = Get-Command git -ErrorAction SilentlyContinue
$branch = "unknown"
$commit = "unknown"
if ($gitAvailable) {
    $branch = (& git branch --show-current 2>$null).Trim()
    $commit = (& git rev-parse HEAD 2>$null).Trim()
    $dirty = [bool]((& git status --porcelain 2>$null) | Select-Object -First 1)
    $results.Add((Write-Check "Git working tree" (-not $dirty) $(if ($dirty) { 'Uncommitted changes exist' } else { 'Clean' })))
} else {
    $results.Add((Write-Check "Git available" $false 'Git is required for source provenance'))
}

$reportDir = Join-Path $repoRoot "build/personal-alpha"
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
$failed = @($results | Where-Object { -not $_.Passed })
$summary = [PSCustomObject]@{
    generatedAt = (Get-Date).ToString("o")
    branch = $branch
    commit = $commit
    strict = [bool]$Strict
    passed = $failed.Count -eq 0
    passedCount = $results.Count - $failed.Count
    failedCount = $failed.Count
    checks = $results
}
$summary | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $reportDir "source-preflight.json")

Write-Host "`nSource preflight: $($results.Count - $failed.Count)/$($results.Count) checks passed."
if ($failed.Count -gt 0) {
    Write-Host "Report: build\personal-alpha\source-preflight.json" -ForegroundColor Yellow
    if ($Strict) { exit 1 }
}
exit 0
