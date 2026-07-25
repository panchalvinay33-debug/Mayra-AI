param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,
    [string]$ManifestPath = "",
    [switch]$AllowSkippedGates
)

$ErrorActionPreference = "Stop"

function Fail([string]$Message) {
    Write-Host "ARTIFACT REJECTED: $Message" -ForegroundColor Red
    exit 1
}

try {
    $apk = (Resolve-Path $ApkPath).Path
} catch {
    Fail "APK not found at '$ApkPath'."
}

if (-not $ManifestPath) {
    $candidate = Join-Path (Split-Path -Parent $apk) "artifact-manifest.json"
    if (Test-Path $candidate) { $ManifestPath = $candidate }
}
if (-not $ManifestPath) {
    Fail "artifact-manifest.json was not found beside the APK."
}
try {
    $manifestFile = (Resolve-Path $ManifestPath).Path
    $manifest = Get-Content $manifestFile -Raw | ConvertFrom-Json
} catch {
    Fail "Artifact manifest is missing or invalid JSON. $($_.Exception.Message)"
}

if ($manifest.schema -ne "mayra.personal-alpha.artifact.v1") {
    Fail "Unsupported artifact manifest schema '$($manifest.schema)'."
}

$sourceSha = [string]$manifest.source.sha
if ($sourceSha -notmatch '^[0-9a-fA-F]{40}$') {
    Fail "Manifest source SHA is missing or invalid."
}

$expectedName = [string]$manifest.artifact.fileName
if ([string]::IsNullOrWhiteSpace($expectedName)) {
    Fail "Manifest artifact file name is missing."
}
if ((Split-Path -Leaf $apk) -ne $expectedName) {
    Fail "APK file name does not match manifest. Expected '$expectedName'."
}

$expectedHash = ([string]$manifest.artifact.sha256).ToLowerInvariant()
if ($expectedHash -notmatch '^[0-9a-f]{64}$') {
    Fail "Manifest APK SHA-256 is missing or invalid."
}
$actualHash = (Get-FileHash -Algorithm SHA256 $apk).Hash.ToLowerInvariant()
if ($actualHash -ne $expectedHash) {
    Fail "APK SHA-256 mismatch. Expected $expectedHash but received $actualHash."
}

$expectedSize = [long]$manifest.artifact.sizeBytes
$actualSize = (Get-Item $apk).Length
if ($expectedSize -le 0 -or $actualSize -ne $expectedSize) {
    Fail "APK size mismatch. Expected $expectedSize bytes but received $actualSize bytes."
}

$requiredGates = @(
    "sourcePreflight",
    "compileDebugKotlin",
    "testDebugUnitTest",
    "lintDebug",
    "assembleDebug"
)
$failedGates = @()
foreach ($gate in $requiredGates) {
    $value = $manifest.gates.$gate
    if ($value -ne $true) { $failedGates += $gate }
}
if ($failedGates.Count -gt 0 -and -not $AllowSkippedGates) {
    Fail "Artifact does not prove all required gates: $($failedGates -join ', ')."
}

$result = [PSCustomObject]@{
    verified = $true
    schema = $manifest.schema
    apkPath = $apk
    manifestPath = $manifestFile
    apkSha256 = $actualHash
    sizeBytes = $actualSize
    sourceRepository = [string]$manifest.source.repository
    sourceRef = [string]$manifest.source.ref
    sourceSha = $sourceSha.ToLowerInvariant()
    gates = $manifest.gates
}

Write-Host "Artifact verified." -ForegroundColor Green
Write-Host "Source SHA: $($result.sourceSha)"
Write-Host "APK SHA-256: $($result.apkSha256)"
$result | ConvertTo-Json -Depth 5
exit 0
