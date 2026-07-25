param(
    [switch]$SkipTests,
    [switch]$SkipLint,
    [switch]$Clean,
    [switch]$NoGradleBootstrap,
    [string]$GradleCommand = ""
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$RequiredGradleVersion = "8.9"
$GradleDistributionSha256 = "d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab"
$GradleDistributionUrl = "https://services.gradle.org/distributions/gradle-$RequiredGradleVersion-bin.zip"

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Write-Info([string]$Message) {
    Write-Host $Message -ForegroundColor DarkGray
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

function Resolve-Java {
    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($java) { return $java.Source }

    $studioCandidates = @(
        (Join-Path $env:ProgramFiles "Android\Android Studio\jbr\bin\java.exe"),
        (Join-Path $env:ProgramFiles "Android\Android Studio\jre\bin\java.exe"),
        (Join-Path ${env:ProgramFiles(x86)} "Android\Android Studio\jbr\bin\java.exe")
    ) | Where-Object { $_ -and (Test-Path $_) }

    if ($studioCandidates.Count -gt 0) {
        $javaPath = $studioCandidates[0]
        $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaPath)
        $env:Path = "$(Split-Path -Parent $javaPath);$env:Path"
        return $javaPath
    }

    return $null
}

function Resolve-AndroidSdk {
    if (-not $env:ANDROID_SDK_ROOT -and $env:ANDROID_HOME) {
        $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    }
    if (-not $env:ANDROID_SDK_ROOT) {
        $defaultSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
        if (Test-Path $defaultSdk) { $env:ANDROID_SDK_ROOT = $defaultSdk }
    }
    return $env:ANDROID_SDK_ROOT
}

function Install-VerifiedGradle([string]$RepoRoot) {
    $toolsRoot = Join-Path $RepoRoot ".tools"
    $gradleHome = Join-Path $toolsRoot "gradle-$RequiredGradleVersion"
    $gradleBat = Join-Path $gradleHome "bin\gradle.bat"
    if (Test-Path $gradleBat) { return $gradleBat }

    if ($NoGradleBootstrap) {
        Fail "Gradle $RequiredGradleVersion was not found and automatic bootstrap was disabled."
    }

    Write-Step "Bootstrapping verified Gradle $RequiredGradleVersion"
    New-Item -ItemType Directory -Force -Path $toolsRoot | Out-Null
    $zipPath = Join-Path $toolsRoot "gradle-$RequiredGradleVersion-bin.zip"
    $tempExtract = Join-Path $toolsRoot "gradle-$RequiredGradleVersion-extracting"

    try {
        if (-not (Test-Path $zipPath)) {
            Write-Info "Downloading $GradleDistributionUrl"
            Invoke-WebRequest -UseBasicParsing -Uri $GradleDistributionUrl -OutFile $zipPath
        }

        $actualHash = (Get-FileHash -Algorithm SHA256 $zipPath).Hash.ToLowerInvariant()
        if ($actualHash -ne $GradleDistributionSha256) {
            Remove-Item -Force $zipPath -ErrorAction SilentlyContinue
            Fail "Gradle download checksum mismatch. Expected $GradleDistributionSha256 but received $actualHash. The file was deleted."
        }

        Remove-Item -Recurse -Force $tempExtract -ErrorAction SilentlyContinue
        Expand-Archive -Path $zipPath -DestinationPath $tempExtract -Force
        $expandedHome = Join-Path $tempExtract "gradle-$RequiredGradleVersion"
        if (-not (Test-Path (Join-Path $expandedHome "bin\gradle.bat"))) {
            Fail "Verified Gradle archive did not contain the expected executable."
        }

        Remove-Item -Recurse -Force $gradleHome -ErrorAction SilentlyContinue
        Move-Item -Path $expandedHome -Destination $gradleHome
        Remove-Item -Recurse -Force $tempExtract -ErrorAction SilentlyContinue
    } catch {
        Fail "Could not bootstrap Gradle $RequiredGradleVersion. Check internet access, antivirus and proxy settings. $($_.Exception.Message)"
    }

    if (-not (Test-Path $gradleBat)) {
        Fail "Gradle bootstrap finished but gradle.bat was not found at $gradleBat"
    }
    return $gradleBat
}

function Resolve-Gradle([string]$RepoRoot) {
    if ($GradleCommand) {
        $explicit = Get-Command $GradleCommand -ErrorAction SilentlyContinue
        if (-not $explicit -and -not (Test-Path $GradleCommand)) {
            Fail "Gradle command '$GradleCommand' was not found."
        }
        return $(if ($explicit) { $explicit.Source } else { (Resolve-Path $GradleCommand).Path })
    }

    $wrapper = Join-Path $RepoRoot "gradlew.bat"
    if (Test-Path $wrapper) { return $wrapper }

    $installed = Get-Command gradle -ErrorAction SilentlyContinue
    if ($installed) { return $installed.Source }

    return Install-VerifiedGradle $RepoRoot
}

function Invoke-GradleStep(
    [string]$GradleExecutable,
    [string[]]$CommonArguments,
    [string[]]$Tasks,
    [string]$LogPath,
    [string]$FailureMessage
) {
    & $GradleExecutable @CommonArguments @Tasks 2>&1 | Tee-Object -FilePath $LogPath
    if ($LASTEXITCODE -ne 0) { Fail $FailureMessage }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$preflight = Join-Path $PSScriptRoot "verify-personal-alpha-source.ps1"
if (-not (Test-Path $preflight)) {
    Fail "Source preflight script is missing: $preflight"
}
Write-Step "Running strict source preflight"
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $preflight -Strict
if ($LASTEXITCODE -ne 0) {
    Fail "Source preflight failed. Review build\personal-alpha\source-preflight.json before compiling."
}

Write-Step "Checking reproducible Windows build environment"

$javaExecutable = Resolve-Java
if (-not $javaExecutable) {
    Fail "Java was not found. Install Android Studio with its embedded JDK 17 or set JAVA_HOME to JDK 17."
}

$javaCheck = Invoke-NativeCapture $javaExecutable @("-version")
if ($javaCheck.ExitCode -ne 0) {
    Fail "Java exists but 'java -version' failed. Check JAVA_HOME and PATH."
}
$javaVersion = ($javaCheck.Output | Select-Object -First 1) -join ""
Write-Host "Java: $javaVersion"
$javaMajorMatch = [regex]::Match($javaVersion, '(?:version\s+")?(?<major>\d+)')
if (-not $javaMajorMatch.Success) {
    Fail "Could not read the Java major version from: $javaVersion"
}
$javaMajor = [int]$javaMajorMatch.Groups["major"].Value
if ($javaMajor -ne 17) {
    Fail "The stabilisation build is locked to JDK 17 for reproducibility. Current Java major version: $javaMajor"
}

$androidSdk = Resolve-AndroidSdk
if (-not $androidSdk -or -not (Test-Path $androidSdk)) {
    Fail "Android SDK was not found. Set ANDROID_SDK_ROOT or install SDK 35 from Android Studio."
}
Write-Host "Android SDK: $androidSdk"

$platform35 = Join-Path $androidSdk "platforms\android-35"
if (-not (Test-Path $platform35)) {
    Fail "Android SDK Platform 35 is missing. Install it from Android Studio > SDK Manager."
}

$gradleExecutable = Resolve-Gradle $repoRoot
Write-Host "Gradle executable: $gradleExecutable"
$gradleVersionCheck = Invoke-NativeCapture $gradleExecutable @("--version")
if ($gradleVersionCheck.ExitCode -ne 0) {
    Fail "Gradle exists but '--version' failed."
}
$gradleVersionText = $gradleVersionCheck.Output -join "`n"
if ($gradleVersionText -notmatch "Gradle\s+$([regex]::Escape($RequiredGradleVersion))(?:\s|$)") {
    Fail "Mayra stabilisation requires Gradle $RequiredGradleVersion. Use the automatic bootstrap or pass the correct -GradleCommand."
}
Write-Host "Gradle: $RequiredGradleVersion"

$env:GRADLE_OPTS = "-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8 -Dkotlin.daemon.jvm.options=-Xmx768m"
$common = @("--no-daemon", "--stacktrace", "--console=plain", "--max-workers=1")

$reportDir = Join-Path $repoRoot "build\personal-alpha"
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

$gitSha = "unknown"
$gitBranch = "unknown"
if (Get-Command git -ErrorAction SilentlyContinue) {
    $shaResult = Invoke-NativeCapture "git" @("rev-parse", "HEAD")
    if ($shaResult.ExitCode -eq 0) { $gitSha = ($shaResult.Output | Select-Object -First 1) }
    $branchResult = Invoke-NativeCapture "git" @("branch", "--show-current")
    if ($branchResult.ExitCode -eq 0) { $gitBranch = ($branchResult.Output | Select-Object -First 1) }
}

$environmentSummary = @"
Mayra AI Personal Alpha Environment
Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Source branch: $gitBranch
Source commit: $gitSha
Java: $javaVersion
Gradle: $RequiredGradleVersion
Android SDK: $androidSdk
Compile SDK: 35
Target SDK: 35
Low-memory mode: max workers 1, Gradle heap 1536 MB
Source preflight: passed
"@
$environmentSummary | Set-Content -Encoding UTF8 (Join-Path $reportDir "environment.txt")

if ($Clean) {
    Write-Step "Cleaning previous build"
    Invoke-GradleStep $gradleExecutable $common @("clean") (Join-Path $reportDir "clean.log") "Gradle clean failed. See build\personal-alpha\clean.log"
}

Write-Step "Compiling Mayra debug sources"
Invoke-GradleStep $gradleExecutable $common @(":app:compileDebugKotlin") (Join-Path $reportDir "compile.log") "Compilation failed. Share build\personal-alpha\compile.log for the exact source fix."

if (-not $SkipTests) {
    Write-Step "Running complete unit tests"
    Invoke-GradleStep $gradleExecutable $common @(":app:testDebugUnitTest") (Join-Path $reportDir "tests.log") "Unit tests failed. Share build\personal-alpha\tests.log."
}

if (-not $SkipLint) {
    Write-Step "Running Android lint"
    Invoke-GradleStep $gradleExecutable $common @(":app:lintDebug") (Join-Path $reportDir "lint.log") "Android lint failed. Share build\personal-alpha\lint.log."
}

Write-Step "Building personal alpha APK"
Invoke-GradleStep $gradleExecutable $common @(":app:assembleDebug") (Join-Path $reportDir "assemble.log") "APK build failed. Share build\personal-alpha\assemble.log."

$apk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) { Fail "Gradle completed but app-debug.apk was not found at $apk" }

$hash = (Get-FileHash -Algorithm SHA256 $apk).Hash
$sizeMb = [Math]::Round((Get-Item $apk).Length / 1MB, 2)
$releaseApkName = "mayra-personal-alpha-$gitSha.apk"
$releaseApk = Join-Path $reportDir $releaseApkName
Copy-Item -Force $apk $releaseApk

$summary = @"
Mayra AI Personal Alpha Build
Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Source branch: $gitBranch
Source commit: $gitSha
APK: $releaseApk
Size MB: $sizeMb
SHA-256: $hash
Source preflight: passed
Compile: passed
Tests skipped: $SkipTests
Lint skipped: $SkipLint
Gradle: $RequiredGradleVersion
JDK: $javaMajor
"@
$summary | Set-Content -Encoding UTF8 (Join-Path $reportDir "build-summary.txt")

Write-Host "`nSUCCESS: Personal alpha APK is ready." -ForegroundColor Green
Write-Host "APK: $releaseApk"
Write-Host "Source commit: $gitSha"
Write-Host "Size: $sizeMb MB"
Write-Host "SHA-256: $hash"
Write-Host "Next: .\scripts\install-personal-alpha.ps1 -ApkPath `"$releaseApk`""
