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

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot
$results = New-Object System.Collections.Generic.List[object]

$requiredFiles = @(
    "settings.gradle.kts",
    "build.gradle.kts",
    "gradle.properties",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/ai/mayra/app/MayraApplication.kt",
    "app/src/main/java/ai/mayra/app/diagnostics/MayraStartupHealth.kt",
    "app/src/main/java/ai/mayra/app/diagnostics/MayraStartupDiagnosticsActivity.kt",
    "app/src/test/java/ai/mayra/app/diagnostics/MayraStartupHealthTest.kt",
    "app/src/main/java/ai/mayra/app/safety/MayraGlobalStop.kt",
    "app/src/test/java/ai/mayra/app/safety/MayraGlobalStopStoreTest.kt",
    "app/src/main/java/ai/mayra/app/action/MayraActionRuntime.kt",
    "app/src/main/java/ai/mayra/app/background/MayraBootReceiver.kt",
    "app/src/main/java/ai/mayra/app/memory/MayraMemoryBackupEngine.kt",
    "app/src/test/java/ai/mayra/app/memory/MayraMemoryBackupEngineTest.kt",
    "app/src/main/java/ai/mayra/app/testing/MayraDeviceTestCenter.kt",
    "app/src/test/java/ai/mayra/app/testing/MayraDeviceTestCenterTest.kt",
    "docs/MAYRA_AI_MASTER_BLUEPRINT.md",
    "docs/MAYRA_LIVING_INTELLIGENCE_VISION.md",
    "docs/MAYRA_SOURCE_OF_TRUTH_AND_BACKUP_MAP.md",
    "docs/PERSONAL_ALPHA_STABILIZATION_STATUS.md",
    "scripts/build-personal-alpha.ps1",
    "scripts/install-personal-alpha.ps1"
)

foreach ($path in $requiredFiles) {
    $exists = Test-Path (Join-Path $repoRoot $path)
    $results.Add((Write-Check "Required file" $exists $path))
}

$settingsText = Get-Content (Join-Path $repoRoot "settings.gradle.kts") -Raw
$results.Add((Write-Check "Android app module" ($settingsText -match 'include\(":app"\)') 'settings.gradle.kts includes :app'))

$appGradle = Get-Content (Join-Path $repoRoot "app/build.gradle.kts") -Raw
$results.Add((Write-Check "Compile SDK" ($appGradle -match 'compileSdk\s*=\s*35') 'compileSdk 35'))
$results.Add((Write-Check "Target SDK" ($appGradle -match 'targetSdk\s*=\s*35') 'targetSdk 35'))
$results.Add((Write-Check "Java target" ($appGradle -match 'JavaVersion\.VERSION_17') 'Java 17 bytecode'))
$results.Add((Write-Check "Version name" ($appGradle -match 'versionName\s*=\s*"0\.1\.0"') 'Personal Alpha V0.1'))

$properties = Get-Content (Join-Path $repoRoot "gradle.properties") -Raw
$boundedMemory = $properties -match 'org\.gradle\.jvmargs=-Xmx(1024|1280|1536)m'
$results.Add((Write-Check "Bounded Gradle memory" $boundedMemory 'Suitable for low-memory Windows build'))
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

$manifest = Get-Content (Join-Path $repoRoot "app/src/main/AndroidManifest.xml") -Raw
$results.Add((Write-Check "Launcher declared" ($manifest -match 'android\.intent\.category\.LAUNCHER') 'Launcher entry exists'))
$results.Add((Write-Check "Internet permission" ($manifest -match 'android\.permission\.INTERNET') 'Optional online AI networking declared'))
$results.Add((Write-Check "Notification permission" ($manifest -match 'android\.permission\.POST_NOTIFICATIONS') 'Android 13+ alerts declared'))
$results.Add((Write-Check "Android auto backup disabled" ($manifest -match 'android:allowBackup="false"') 'Sensitive state uses explicit Mayra backup only'))
$results.Add((Write-Check "Full backup disabled" ($manifest -match 'android:fullBackupContent="false"') 'No implicit cloud extraction of Mayra data'))
$results.Add((Write-Check "Cleartext traffic disabled" ($manifest -match 'android:usesCleartextTraffic="false"') 'Network providers must use encrypted transport'))
$results.Add((Write-Check "Startup diagnostics private" ($manifest -match 'MayraStartupDiagnosticsActivity" android:exported="false"') 'Startup health is owner-only'))
$results.Add((Write-Check "Internal components protected" (-not ($manifest -match '<activity[^>]+android:exported="true"[^>]*>' -and $manifest -notmatch 'MayraPresenceActivity')) 'Only the launcher is externally exported'))

$applicationText = Get-Content (Join-Path $repoRoot "app/src/main/java/ai/mayra/app/MayraApplication.kt") -Raw
$results.Add((Write-Check "Startup health begins" ($applicationText -match 'startupHealth\.begin\(\)') 'Interrupted starts can be detected'))
$results.Add((Write-Check "Startup health completes" ($applicationText -match 'startupHealth\.complete\(\)') 'Successful starts are marked complete'))
$safeStartupSteps = [regex]::Matches($applicationText, 'startupHealth\.safeStep\(').Count
$results.Add((Write-Check "Non-critical startup containment" ($safeStartupSteps -ge 10) "Found $safeStartupSteps guarded startup steps"))

$globalStopText = Get-Content (Join-Path $repoRoot "app/src/main/java/ai/mayra/app/safety/MayraGlobalStop.kt") -Raw
$actionRuntimeText = Get-Content (Join-Path $repoRoot "app/src/main/java/ai/mayra/app/action/MayraActionRuntime.kt") -Raw
$bootReceiverText = Get-Content (Join-Path $repoRoot "app/src/main/java/ai/mayra/app/background/MayraBootReceiver.kt") -Raw
$results.Add((Write-Check "Persistent Global Stop" ($globalStopText -match 'mayra_global_stop' -and $globalStopText -match 'KEY_GENERATION') 'Stop state survives process death, reboot and update'))
$results.Add((Write-Check "Action runtime obeys Global Stop" ($actionRuntimeText -match 'MayraGlobalStopStore' -and $actionRuntimeText -match 'store\.isStopped\(\)') 'Action engine restores persisted stop state'))
$results.Add((Write-Check "Boot path obeys Global Stop" ($bootReceiverText -match 'MayraGlobalStopStore' -and $bootReceiverText -match 'if \(globallyStopped\)') 'Automation and floating companion remain stopped after reboot'))
$results.Add((Write-Check "Reminders survive Global Stop" ($bootReceiverText -match 'MayraReminderRuntime\.rescheduleAll' -and $bootReceiverText.IndexOf('MayraReminderRuntime.rescheduleAll') -lt $bootReceiverText.IndexOf('if (globallyStopped)')) 'Owner-created reminder commitments remain scheduled'))

$backupText = Get-Content (Join-Path $repoRoot "app/src/main/java/ai/mayra/app/memory/MayraMemoryBackupEngine.kt") -Raw
$results.Add((Write-Check "Authenticated backup encryption" ($backupText -match 'AES/GCM/NoPadding') 'Backup uses AES-GCM'))
$results.Add((Write-Check "Password key derivation" ($backupText -match 'PBKDF2WithHmacSHA256') 'Backup key uses PBKDF2-HMAC-SHA256'))
$results.Add((Write-Check "Versioned backup envelope" ($backupText -match 'MAYRA_ENCRYPTED_BACKUP_V1') 'Restore can reject unsupported formats'))

$deviceTestText = Get-Content (Join-Path $repoRoot "app/src/main/java/ai/mayra/app/testing/MayraDeviceTestCenter.kt") -Raw
$deviceIds = [regex]::Matches($deviceTestText, 'DeviceTestDefinition\(DeviceTestId\.')
$results.Add((Write-Check "Twenty physical alpha checks" ($deviceIds.Count -eq 20) "Found $($deviceIds.Count) DeviceTestDefinition entries"))
$results.Add((Write-Check "Floating Mayra acceptance" ($deviceTestText -match 'DeviceTestId\.FLOATING_MAYRA') 'Overlay lifecycle is physically tested'))
$results.Add((Write-Check "Backup restore acceptance" ($deviceTestText -match 'DeviceTestId\.MEMORY_BACKUP_RESTORE') 'Encrypted backup and restore are physically tested'))

$gitAvailable = Get-Command git -ErrorAction SilentlyContinue
$branch = "unknown"
$commit = "unknown"
$dirty = $false
if ($gitAvailable) {
    $branch = (& git branch --show-current 2>$null).Trim()
    $commit = (& git rev-parse HEAD 2>$null).Trim()
    $dirty = [bool]((& git status --porcelain 2>$null) | Select-Object -First 1)
    $dirtyDetail = if ($dirty) { "Uncommitted changes exist" } else { "Clean" }
    $results.Add((Write-Check "Git working tree" (-not $dirty) $dirtyDetail))
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
    checks = $results
}
$summary | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $reportDir "source-preflight.json")

Write-Host "`nSource preflight: $($results.Count - $failed.Count)/$($results.Count) checks passed."
if ($failed.Count -gt 0) {
    Write-Host "Report: build\personal-alpha\source-preflight.json" -ForegroundColor Yellow
    if ($Strict) { exit 1 }
}

exit 0
