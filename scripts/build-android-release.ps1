[CmdletBinding()]
param(
    [string]$ProjectRoot = "D:\Projects\CalorieTracker",
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

$resolvedProjectRoot = (Resolve-Path $ProjectRoot).Path
$resolvedAndroidRoot = (Resolve-Path $AndroidRoot).Path
$releasePropertiesPath = Join-Path $resolvedAndroidRoot "release.properties"
$securePropertiesPath = Join-Path $resolvedAndroidRoot "secure.properties"
$bundleOutputPath = Join-Path $resolvedAndroidRoot "app\build\outputs\bundle\prodRelease\app-prod-release.aab"

Assert-FileExists $releasePropertiesPath "Missing apps/android/release.properties. Copy apps/android/release.properties.example and fill in the signing values first."
Assert-FileExists $securePropertiesPath "Missing apps/android/secure.properties. Copy apps/android/secure.properties.example and fill in the Cognito/API values first."

Write-Host "Building BiteWise prod Android release bundle..."
Write-Host "Android root: $resolvedAndroidRoot"

Push-Location $resolvedAndroidRoot
try {
    if ($Clean) {
        & .\gradlew.bat clean
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle clean failed with exit code $LASTEXITCODE."
        }
    }

    & .\gradlew.bat :app:bundleProdRelease
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle bundleProdRelease failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

Assert-FileExists $bundleOutputPath "Expected bundle output was not found at $bundleOutputPath."

Write-Host ""
Write-Host "Release bundle created:"
Write-Host $bundleOutputPath
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Open Google Play Console."
Write-Host "2. Go to your BiteWise app."
Write-Host "3. Create or open an Internal, Closed, or Production release."
Write-Host "4. Upload the .aab shown above."
Write-Host "5. Save the release and continue through the Play Console checklist."
