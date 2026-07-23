@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem Mayra AI Gradle bootstrap for Windows.
rem Downloads the pinned Gradle distribution on first use, then forwards all arguments.

set "GRADLE_VERSION=8.9"
set "DIST_NAME=gradle-%GRADLE_VERSION%-bin"
set "CACHE_ROOT=%USERPROFILE%\.gradle\mayra-wrapper"
set "ZIP_PATH=%CACHE_ROOT%\%DIST_NAME%.zip"
set "GRADLE_HOME=%CACHE_ROOT%\gradle-%GRADLE_VERSION%"
set "GRADLE_BAT=%GRADLE_HOME%\bin\gradle.bat"
set "DIST_URL=https://services.gradle.org/distributions/%DIST_NAME%.zip"

if exist "%GRADLE_BAT%" goto run_gradle

echo.
echo [Mayra AI] Gradle %GRADLE_VERSION% is not installed in the local wrapper cache.
echo [Mayra AI] Downloading from %DIST_URL%

if not exist "%CACHE_ROOT%" mkdir "%CACHE_ROOT%"
if errorlevel 1 goto failed

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12;" ^
  "Invoke-WebRequest -UseBasicParsing -Uri '%DIST_URL%' -OutFile '%ZIP_PATH%';" ^
  "if (Test-Path '%GRADLE_HOME%') { Remove-Item -Recurse -Force '%GRADLE_HOME%' };" ^
  "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%CACHE_ROOT%' -Force"
if errorlevel 1 goto download_failed

if not exist "%GRADLE_BAT%" (
  echo [Mayra AI] Gradle extraction completed, but gradle.bat was not found.
  goto failed
)

:run_gradle
call "%GRADLE_BAT%" %*
exit /b %ERRORLEVEL%

:download_failed
echo.
echo [Mayra AI] Gradle download failed. Check the internet connection and try again.
if exist "%ZIP_PATH%" del /q "%ZIP_PATH%" >nul 2>&1
exit /b 1

:failed
echo.
echo [Mayra AI] Gradle bootstrap could not be prepared.
exit /b 1
