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
    "docs/MAYRA_AI_MASTER_BLUEPRINT.md",
    "docs/MAYRA_LIVING_INTELLIGENCE_VISION.md",
    "docs/MAYRA_SOURCE_OF_TRUTH_AND_BACKUP_MAP.md",
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
    '(?i)api[_-]?key\s*[=:]\s*["''][^"'']{12,}',
    '(?i)client[_-]?secret\s*[=:]\s*["''][^"'']{12,}',
    '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----'
)
$scanExtensions = @("*.kt", "*.kts", "*.java", "*.xml", "*.json", "*.md", "*.yml", "*.yaml", "*.properties", "*.ps1")
$secretHits = New-Object System.Collections.Generic.List[string]
foreach ($extension in $scanExtensions) {
    Get-ChildItem -Path $repoRoot -Recurse -File -Filter $extension -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notmatch '[\\/](build|\.gradle|\.git)[\\/]' } |
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

$gitAvailable = Get-Command git -ErrorAction SilentlyContinue
$branch = "unknown"
$commit = "unknown"
$dirty = $false
if ($gitAvailable) {
    $branch = (& git branch --show-current 2>$null).Trim()
    $commit = (& git rev-parse HEAD 2>$null).Trim()
    $dirty = [bool]((& git status --porcelain 2>$null) | Select-Object -First 1)
    $results.Add((Write-Check "Git working tree" (-not $dirty) (if ($dirty) { "Uncommitted changes exist" } else { "Clean" })))
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
