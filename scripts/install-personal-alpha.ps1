param(
    [switch]$FreshInstall,
    [switch]$GrantCommonPermissions,
    [string]$DeviceSerial = ""
)

$ErrorActionPreference = "Stop"

function Fail([string]$Message) {
    Write-Host "`nINSTALL BLOCKED: $Message" -ForegroundColor Red
    exit 1
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$apk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) {
    Fail "APK not found. Run .\scripts\build-personal-alpha.ps1 first."
}

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

$devices = & $adbPath devices
$ready = $devices | Select-String "\tdevice$"
if (-not $ready) {
    Fail "No authorised Android phone found. Connect USB, enable Developer options > USB debugging, and accept the phone authorisation prompt."
}

$serialArgs = @()
if ($DeviceSerial) { $serialArgs = @("-s", $DeviceSerial) }

Write-Host "Installing Mayra AI personal alpha..." -ForegroundColor Cyan
if ($FreshInstall) {
    Write-Host "Fresh install selected: existing Mayra app data will be removed." -ForegroundColor Yellow
    & $adbPath @serialArgs uninstall ai.mayra.app | Out-Host
}

& $adbPath @serialArgs install -r -t $apk | Out-Host
if ($LASTEXITCODE -ne 0) { Fail "APK installation failed. Review the adb output above." }

if ($GrantCommonPermissions) {
    Write-Host "Granting common runtime permissions supported by the phone..." -ForegroundColor Cyan
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
    }
}

Write-Host "Launching Mayra..." -ForegroundColor Cyan
& $adbPath @serialArgs shell monkey -p ai.mayra.app -c android.intent.category.LAUNCHER 1 | Out-Null

Write-Host "`nSUCCESS: Mayra AI installed and launch requested." -ForegroundColor Green
Write-Host "On the phone, complete onboarding and open 'Start personal device check'."
Write-Host "Notification Access, battery settings and default-app roles must still be enabled manually in Android Settings."
