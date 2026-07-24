param(
    [switch]$SkipTests,
    [switch]$SkipLint,
    [switch]$Clean,
    [string]$GradleCommand = "gradle"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Fail([string]$Message) {
    Write-Host "`nBUILD BLOCKED: $Message" -ForegroundColor Red
    exit 1
}

function Invoke-NativeCapture([string]$Executable, [string[]]$Arguments) {
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $Executable @Arguments 2>&1 | ForEach-Object { $_.ToString() }
        $exitCode = $LASTEXITCODE
        return [PSCustomObject]@{ Output = $output; ExitCode = $exitCode }
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

Write-Step "Checking Windows build environment"

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Fail "Java was not found. Point JAVA_HOME to Android Studio's embedded JDK or install JDK 17."
}

$javaCheck = Invoke-NativeCapture "java" @("-version")
if ($javaCheck.ExitCode -ne 0) {
    Fail "Java exists but 'java -version' failed. Check JAVA_HOME and PATH."
}
$javaVersion = ($javaCheck.Output | Select-Object -First 1) -join ""
Write-Host "Java: $javaVersion"
$javaMajorMatch = [regex]::Match($javaVersion, 'version\s+"(?<major>\d+)')
if (-not $javaMajorMatch.Success) {
    Fail "Could not read the Java major version from: $javaVersion"
}
$javaMajor = [int]$javaMajorMatch.Groups["major"].Value
if ($javaMajor -lt 17 -or $javaMajor -gt 21) {
    Fail "Mayra requires JDK 17 through 21 for this personal build. Current Java major version: $javaMajor"
}
if ($javaMajor -ne 17) {
    Write-Host "Note: JDK $javaMajor detected. The project targets Java 17 bytecode; Gradle 8.9/AGP 8.7 can run on this JDK." -ForegroundColor Yellow
}

if (-not $env:ANDROID_SDK_ROOT -and $env:ANDROID_HOME) {
    $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
}
if (-not $env:ANDROID_SDK_ROOT) {
    $defaultSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $defaultSdk) { $env:ANDROID_SDK_ROOT = $defaultSdk }
}
if (-not $env:ANDROID_SDK_ROOT -or -not (Test-Path $env:ANDROID_SDK_ROOT)) {
    Fail "Android SDK was not found. Set ANDROID_SDK_ROOT or install SDK 35 from Android Studio."
}
Write-Host "Android SDK: $env:ANDROID_SDK_ROOT"

$platform35 = Join-Path $env:ANDROID_SDK_ROOT "platforms\android-35"
if (-not (Test-Path $platform35)) {
    Fail "Android SDK Platform 35 is missing. Install it from Android Studio > SDK Manager."
}

$gradle = Get-Command $GradleCommand -ErrorAction SilentlyContinue
if (-not $gradle) {
    Fail "Gradle command '$GradleCommand' was not found. Open this project in Android Studio and run the Gradle task there, or install Gradle 8.9 and add it to PATH."
}
Write-Host "Gradle: $($gradle.Source)"

$env:GRADLE_OPTS = "-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8 -Dkotlin.daemon.jvm.options=-Xmx768m"
$common = @("--no-daemon", "--stacktrace", "--console=plain", "--max-workers=1")

$reportDir = Join-Path $repoRoot "build\personal-alpha"
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

if ($Clean) {
    Write-Step "Cleaning previous build"
    & $GradleCommand @common "clean" 2>&1 | Tee-Object -FilePath (Join-Path $reportDir "clean.log")
    if ($LASTEXITCODE -ne 0) { Fail "Gradle clean failed. See build\personal-alpha\clean.log" }
}

Write-Step "Compiling Mayra debug sources"
& $GradleCommand @common ":app:compileDebugKotlin" 2>&1 | Tee-Object -FilePath (Join-Path $reportDir "compile.log")
if ($LASTEXITCODE -ne 0) { Fail "Compilation failed. Share build\personal-alpha\compile.log for the exact source fix." }

if (-not $SkipTests) {
    Write-Step "Running unit tests"
    & $GradleCommand @common ":app:testDebugUnitTest" 2>&1 | Tee-Object -FilePath (Join-Path $reportDir "tests.log")
    if ($LASTEXITCODE -ne 0) { Fail "Unit tests failed. Share build\personal-alpha\tests.log." }
}

if (-not $SkipLint) {
    Write-Step "Running Android lint"
    & $GradleCommand @common ":app:lintDebug" 2>&1 | Tee-Object -FilePath (Join-Path $reportDir "lint.log")
    if ($LASTEXITCODE -ne 0) { Fail "Android lint failed. Share build\personal-alpha\lint.log." }
}

Write-Step "Building personal alpha APK"
& $GradleCommand @common ":app:assembleDebug" 2>&1 | Tee-Object -FilePath (Join-Path $reportDir "assemble.log")
if ($LASTEXITCODE -ne 0) { Fail "APK build failed. Share build\personal-alpha\assemble.log." }

$apk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) { Fail "Gradle completed but app-debug.apk was not found at $apk" }

$hash = (Get-FileHash -Algorithm SHA256 $apk).Hash
$sizeMb = [Math]::Round((Get-Item $apk).Length / 1MB, 2)
$summary = @"
Mayra AI Personal Alpha Build
Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Branch expected: batch-12-runtime-control-center
APK: $apk
Size MB: $sizeMb
SHA-256: $hash
Tests skipped: $SkipTests
Lint skipped: $SkipLint
"@
$summary | Set-Content -Encoding UTF8 (Join-Path $reportDir "build-summary.txt")

Write-Host "`nSUCCESS: Personal alpha APK is ready." -ForegroundColor Green
Write-Host "APK: $apk"
Write-Host "Size: $sizeMb MB"
Write-Host "SHA-256: $hash"
Write-Host "Next: .\scripts\install-personal-alpha.ps1"