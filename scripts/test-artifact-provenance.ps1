param()

$ErrorActionPreference = "Stop"

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "ASSERTION FAILED: $Message" }
}

function Invoke-Verifier(
    [string]$Verifier,
    [string]$Apk,
    [string]$Manifest,
    [switch]$AllowSkipped
) {
    $arguments = @(
        "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", $Verifier,
        "-ApkPath", $Apk,
        "-ManifestPath", $Manifest
    )
    if ($AllowSkipped) { $arguments += "-AllowSkippedGates" }
    & powershell.exe @arguments *> $null
    return $LASTEXITCODE
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$verifier = Join-Path $PSScriptRoot "verify-personal-alpha-artifact.ps1"
if (-not (Test-Path $verifier)) { throw "Verifier not found: $verifier" }

$temp = Join-Path ([System.IO.Path]::GetTempPath()) ("mayra-artifact-test-" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $temp | Out-Null
try {
    $apkName = "mayra-personal-alpha-0123456789012345678901234567890123456789.apk"
    $apk = Join-Path $temp $apkName
    [System.IO.File]::WriteAllBytes($apk, [byte[]](1, 2, 3, 4, 5, 6, 7, 8))
    $hash = (Get-FileHash -Algorithm SHA256 $apk).Hash.ToLowerInvariant()
    $size = (Get-Item $apk).Length
    $manifestPath = Join-Path $temp "artifact-manifest.json"

    function Write-Manifest([bool]$Tests = $true, [string]$FileName = $apkName, [string]$Sha = $hash) {
        $manifest = [PSCustomObject]@{
            schema = "mayra.personal-alpha.artifact.v1"
            source = [PSCustomObject]@{
                repository = "panchalvinay33-debug/Mayra-AI"
                ref = "test"
                sha = "0123456789012345678901234567890123456789"
            }
            gates = [PSCustomObject]@{
                sourcePreflight = $true
                compileDebugKotlin = $true
                testDebugUnitTest = $Tests
                lintDebug = $true
                assembleDebug = $true
            }
            artifact = [PSCustomObject]@{
                fileName = $FileName
                sha256 = $Sha
                sizeBytes = $size
            }
        }
        $manifest | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 $manifestPath
    }

    Write-Manifest
    Assert-True ((Invoke-Verifier $verifier $apk $manifestPath) -eq 0) "Valid artifact must pass."

    Add-Content -Encoding Byte -Path $apk -Value 9
    Assert-True ((Invoke-Verifier $verifier $apk $manifestPath) -ne 0) "Tampered APK must fail."
    [System.IO.File]::WriteAllBytes($apk, [byte[]](1, 2, 3, 4, 5, 6, 7, 8))

    Write-Manifest -FileName "wrong.apk"
    Assert-True ((Invoke-Verifier $verifier $apk $manifestPath) -ne 0) "Wrong file name must fail."

    Write-Manifest -Tests $false
    Assert-True ((Invoke-Verifier $verifier $apk $manifestPath) -ne 0) "Skipped required gate must fail by default."
    Assert-True ((Invoke-Verifier $verifier $apk $manifestPath -AllowSkipped) -eq 0) "Explicit diagnostic override must allow skipped gates."

    Write-Manifest -Sha ("0" * 64)
    Assert-True ((Invoke-Verifier $verifier $apk $manifestPath) -ne 0) "Wrong hash must fail."

    Write-Host "Artifact provenance regression checks passed." -ForegroundColor Green
} finally {
    Remove-Item -Recurse -Force $temp -ErrorAction SilentlyContinue
}
