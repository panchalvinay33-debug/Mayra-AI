param(
    [switch]$FreshInstall,
    [switch]$GrantCommonPermissions,
    [string]$DeviceSerial = "",
    [string]$ApkPath = ""
)

$ErrorActionPreference = "Stop"

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Fail([string]$Message) {
    Write-Host "`nINSTALL BLOCKED: $Message" -ForegroundColor Red
    exit 1
}

function Invoke-Adb([string]$AdbPath, [string[]]$SerialArguments, [string[]]$Arguments) {
    & $AdbPath @SerialArguments @Arguments
    return $LASTEXITCODE
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

$apkHash = (Get-FileHash -Algorithm SHA256 $apk).Hash
$apkSizeMb = [Math]::Round((Get-Item $apk).Length / 1MB, 2)
Write-Host "APK: $apk"
Write-Host "Size: $apkSizeMb MB"
Write-Host "SHA-256: $apkHash"

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
        "android.permission.CALL_PHONE",
        "android.permission.SEND_SMS",
        "android.permission.POST_NOTIFICATIONS"
    )
    foreach ($permission in $permissions) {
        & $adbPath @serialArgs shell pm grant ai.mayra.app $permission 2>$null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Permission requires manual review or is unsupported: $permission" -ForegroundColor Yellow
        }
    }
}

Write-Step "Launching Mayra"
& $adbPath @serialArgs shell monkey -p ai.mayra.app -c android.intent.category.LAUNCHER 1 | Out-Null
if ($LASTEXITCODE -ne 0) { Fail "Mayra was installed but the launcher request failed." }

$installReportDir = Join-Path $repoRoot "build\personal-alpha"
New-Item -ItemType Directory -Force -Path $installReportDir | Out-Null
$installSummary = @"
Mayra AI Personal Alpha Installation
Installed: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Device serial: $DeviceSerial
Device: $manufacturer $model
Android: $androidVersion
SDK: $sdkLevel
APK: $apk
APK SHA-256: $apkHash
Fresh install: $FreshInstall
Common permissions requested: $GrantCommonPermissions
"@
$installSummary | Set-Content -Encoding UTF8 (Join-Path $installReportDir "install-summary.txt")

Write-Host "`nSUCCESS: Mayra AI installed and launch requested." -ForegroundColor Green
Write-Host "On the phone, complete onboarding and open 'Start personal device check'."
Write-Host "Notification Access, overlay, Accessibility and battery settings must still be enabled manually from Mayra Access."
