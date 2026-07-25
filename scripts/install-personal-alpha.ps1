param(
    [switch]$FreshInstall,
    [switch]$GrantCommonPermissions,
    [switch]$AllowUnverifiedArtifact,
    [string]$DeviceSerial = "",
    [string]$ApkPath = "",
    [string]$ArtifactManifestPath = ""
)

$ErrorActionPreference = "Stop"

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Fail([string]$Message) {
    Write-Host "`nINSTALL BLOCKED: $Message" -ForegroundColor Red
    exit 1
}

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $ApkPath) {
    $releaseDir = Join-Path $repoRoot "build\personal-alpha"
    $latestReleaseApk = Get-ChildItem -Path $releaseDir -Filter "mayra-personal-alpha-*.apk" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1

    if ($latestReleaseApk) {
        $ApkPath = $latestReleaseApk.FullName
    } else {
        $ApkPath = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
    }
}

try {
    $apk = (Resolve-Path $ApkPath).Path
} catch {
    Fail "APK not found at '$ApkPath'. Run .\scripts\build-personal-alpha.ps1 first."
}

$manifestCandidate = if ($ArtifactManifestPath) {
    $ArtifactManifestPath
} else {
    Join-Path (Split-Path -Parent $apk) "artifact-manifest.json"
}
$verifiedArtifact = $false
$sourceSha = "unverified"
if (Test-Path $manifestCandidate) {
    Write-Step "Verifying APK provenance"
    $verifyScript = Join-Path $PSScriptRoot "verify-personal-alpha-artifact.ps1"
    if (-not (Test-Path $verifyScript)) { Fail "Artifact verifier is missing: $verifyScript" }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $verifyScript -ApkPath $apk -ManifestPath $manifestCandidate
    if ($LASTEXITCODE -ne 0) { Fail "APK provenance verification failed." }
    $artifactManifest = Get-Content $manifestCandidate -Raw | ConvertFrom-Json
    $sourceSha = [string]$artifactManifest.source.sha
    $verifiedArtifact = $true
} elseif (-not $AllowUnverifiedArtifact) {
    Fail "artifact-manifest.json was not found beside the APK. Build again with the controlled Personal Alpha script, or explicitly use -AllowUnverifiedArtifact for a non-release diagnostic install."
} else {
    Write-Host "WARNING: Installing an APK without verified provenance." -ForegroundColor Yellow
}

$apkHash = (Get-FileHash -Algorithm SHA256 $apk).Hash.ToLowerInvariant()
$apkSizeBytes = (Get-Item $apk).Length
$apkSizeMb = [Math]::Round($apkSizeBytes / 1MB, 2)
Write-Host "APK: $apk"
Write-Host "Size: $apkSizeMb MB"
Write-Host "SHA-256: $apkHash"
Write-Host "Source SHA: $sourceSha"

$adb = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adb) {
    $sdk = $env:ANDROID_SDK_ROOT
    if (-not $sdk -and $env:ANDROID_HOME) { $sdk = $env:ANDROID_HOME }
    if (-not $sdk) {
        $defaultSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
        if (Test-Path $defaultSdk) { $sdk = $defaultSdk }
    }
    if ($sdk) {
        $candidate = Join-Path $sdk "platform-tools\adb.exe"
        if (Test-Path $candidate) { $adb = Get-Item $candidate }
    }
}
if (-not $adb) { Fail "adb was not found. Install Android SDK Platform Tools and enable USB debugging." }

$adbPath = $adb.Source
if (-not $adbPath) { $adbPath = $adb.FullName }
& $adbPath start-server | Out-Null

$deviceRows = & $adbPath devices | Select-String "\tdevice$"
if (-not $deviceRows) {
    Fail "No authorised Android phone found. Connect USB, enable Developer options > USB debugging, and accept the phone authorisation prompt."
}

$connectedSerials = @($deviceRows | ForEach-Object { ($_.Line -split "\t")[0].Trim() })
if ($DeviceSerial) {
    if ($connectedSerials -notcontains $DeviceSerial) {
        Fail "Requested device '$DeviceSerial' is not in the authorised device list: $($connectedSerials -join ', ')"
    }
} elseif ($connectedSerials.Count -gt 1) {
    Fail "Multiple authorised phones are connected. Re-run with -DeviceSerial <serial>. Devices: $($connectedSerials -join ', ')"
} else {
    $DeviceSerial = $connectedSerials[0]
}

$serialArgs = @("-s", $DeviceSerial)
Write-Host "Target device: $DeviceSerial"

Write-Step "Capturing device details"
$manufacturer = (& $adbPath @serialArgs shell getprop ro.product.manufacturer).Trim()
$model = (& $adbPath @serialArgs shell getprop ro.product.model).Trim()
$androidVersion = (& $adbPath @serialArgs shell getprop ro.build.version.release).Trim()
$sdkLevel = (& $adbPath @serialArgs shell getprop ro.build.version.sdk).Trim()
$buildFingerprint = (& $adbPath @serialArgs shell getprop ro.build.fingerprint).Trim()
Write-Host "Phone: $manufacturer $model"
Write-Host "Android: $androidVersion (SDK $sdkLevel)"

Write-Step "Installing Mayra AI personal alpha"
if ($FreshInstall) {
    Write-Host "Fresh install selected: existing Mayra app data will be removed." -ForegroundColor Yellow
    & $adbPath @serialArgs uninstall ai.mayra.app | Out-Host
}

& $adbPath @serialArgs install -r -t $apk | Out-Host
if ($LASTEXITCODE -ne 0) { Fail "APK installation failed. Review the adb output above." }

if ($GrantCommonPermissions) {
    Write-Step "Granting common runtime permissions supported by the phone"
    $permissions = @(
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.READ_CONTACTS",
        "android.permission.POST_NOTIFICATIONS"
    )
    foreach ($permission in $permissions) {
        & $adbPath @serialArgs shell pm grant ai.mayra.app $permission 2>$null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Permission requires manual review or is unsupported: $permission" -ForegroundColor Yellow
        }
    }
}

Write-Step "Verifying installed package"
$packagePath = (& $adbPath @serialArgs shell pm path ai.mayra.app).Trim()
if ($LASTEXITCODE -ne 0 -or $packagePath -notmatch '^package:') {
    Fail "Android did not report the installed ai.mayra.app package."
}
$packageDump = (& $adbPath @serialArgs shell dumpsys package ai.mayra.app) -join "`n"
$versionName = ([regex]::Match($packageDump, 'versionName=([^\s]+)')).Groups[1].Value
$versionCode = ([regex]::Match($packageDump, 'versionCode=(\d+)')).Groups[1].Value

Write-Step "Launching Mayra"
& $adbPath @serialArgs shell monkey -p ai.mayra.app -c android.intent.category.LAUNCHER 1 | Out-Null
if ($LASTEXITCODE -ne 0) { Fail "Mayra was installed but the launcher request failed." }

$installReportDir = Join-Path $repoRoot "build\personal-alpha"
New-Item -ItemType Directory -Force -Path $installReportDir | Out-Null
$installedAt = (Get-Date).ToString("o")
$installData = [PSCustomObject]@{
    schema = "mayra.personal-alpha.install.v1"
    installedAt = $installedAt
    sourceSha = $sourceSha
    artifactVerified = $verifiedArtifact
    apkPath = $apk
    apkSha256 = $apkHash
    apkSizeBytes = $apkSizeBytes
    device = [PSCustomObject]@{
        serial = $DeviceSerial
        manufacturer = $manufacturer
        model = $model
        androidVersion = $androidVersion
        sdk = $sdkLevel
        buildFingerprint = $buildFingerprint
    }
    package = [PSCustomObject]@{
        name = "ai.mayra.app"
        path = $packagePath
        versionName = $versionName
        versionCode = $versionCode
    }
    options = [PSCustomObject]@{
        freshInstall = [bool]$FreshInstall
        commonPermissionsRequested = [bool]$GrantCommonPermissions
    }
}
$installData | ConvertTo-Json -Depth 6 | Set-Content -Encoding UTF8 (Join-Path $installReportDir "install-manifest.json")

Write-Host "`nSUCCESS: Mayra AI installed and launch requested." -ForegroundColor Green
Write-Host "Installed package: ai.mayra.app $versionName ($versionCode)"
Write-Host "Install evidence: build\personal-alpha\install-manifest.json"
Write-Host "On the phone, complete onboarding and open 'Start personal device check'."
Write-Host "Notification Access, overlay, Accessibility and battery settings must still be enabled manually from Mayra Access."
