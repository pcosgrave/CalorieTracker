[CmdletBinding()]
param(
    [string]$AndroidRoot = "D:\Projects\CalorieTracker\apps\android",
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

function Assert-FileExists {
    param(
        [string]$Path,
        [string]$Message
    )

    if (-not (Test-Path $Path)) {
        throw $Message
    }
}

$resolvedAndroidRoot = (Resolve-Path $AndroidRoot).Path
$securePropertiesPath = Join-Path $resolvedAndroidRoot "secure.properties"
$apkOutputPath = Join-Path $resolvedAndroidRoot "app\build\outputs\apk\dev\debug\app-dev-debug.apk"

Assert-FileExists $securePropertiesPath "Missing apps/android/secure.properties. Copy apps/android/secure.properties.example and fill in the Cognito/API values first."

Write-Host "Building BiteWise Dev Android debug APK..."
Write-Host "Android root: $resolvedAndroidRoot"

Push-Location $resolvedAndroidRoot
try {
    if ($Clean) {
        & .\gradlew.bat clean
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle clean failed with exit code $LASTEXITCODE."
        }
    }

    & .\gradlew.bat :app:assembleDevDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle assembleDevDebug failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

Assert-FileExists $apkOutputPath "Expected APK output was not found at $apkOutputPath."

Write-Host ""
Write-Host "Dev APK created:"
Write-Host $apkOutputPath
Write-Host ""
Write-Host "Suggested usage:"
Write-Host "1. Install this APK on your device or emulator."
Write-Host "2. Use the dev Cognito callback scheme bitewise-dev://auth/callback."
Write-Host "3. Keep this build for local testing while prod waits on Google verification."
