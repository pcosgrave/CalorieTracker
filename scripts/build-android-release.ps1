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

function Get-RequiredMatchValue {
    param(
        [string]$Content,
        [string]$Pattern,
        [string]$Description
    )

    $match = [regex]::Match($Content, $Pattern)
    if (-not $match.Success) {
        throw "Unable to find $Description in build.gradle.kts."
    }

    return $match.Groups[1].Value
}

$resolvedProjectRoot = (Resolve-Path $ProjectRoot).Path
$resolvedAndroidRoot = (Resolve-Path $AndroidRoot).Path
$releasePropertiesPath = Join-Path $resolvedAndroidRoot "release.properties"
$securePropertiesPath = Join-Path $resolvedAndroidRoot "secure.properties"
$gradleFilePath = Join-Path $resolvedAndroidRoot "app\build.gradle.kts"
$bundleOutputPath = Join-Path $resolvedAndroidRoot "app\build\outputs\bundle\prodRelease\app-prod-release.aab"
$releaseArchiveRoot = Join-Path $resolvedProjectRoot "releases\android"

Assert-FileExists $releasePropertiesPath "Missing apps/android/release.properties. Copy apps/android/release.properties.example and fill in the signing values first."
Assert-FileExists $securePropertiesPath "Missing apps/android/secure.properties. Copy apps/android/secure.properties.example and fill in the Cognito/API values first."
Assert-FileExists $gradleFilePath "Missing apps/android/app/build.gradle.kts."

$gradleFileContent = Get-Content -Path $gradleFilePath -Raw
$versionCode = Get-RequiredMatchValue -Content $gradleFileContent -Pattern 'versionCode\s*=\s*(\d+)' -Description 'versionCode'
$versionName = Get-RequiredMatchValue -Content $gradleFileContent -Pattern 'versionName\s*=\s*"([^"]+)"' -Description 'versionName'
$applicationId = Get-RequiredMatchValue -Content $gradleFileContent -Pattern 'applicationId\s*=\s*"([^"]+)"' -Description 'applicationId'

$releaseTag = "v$versionName+$versionCode"
$archiveFileName = "BiteWise-$releaseTag.aab"
$archiveBundlePath = Join-Path $releaseArchiveRoot $archiveFileName
$archiveMetadataPath = Join-Path $releaseArchiveRoot ("BiteWise-$releaseTag.json")

Write-Host "Building BiteWise prod Android release bundle..."
Write-Host "Android root: $resolvedAndroidRoot"
Write-Host "Version tag: $releaseTag"

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

New-Item -ItemType Directory -Force -Path $releaseArchiveRoot | Out-Null
Copy-Item -Path $bundleOutputPath -Destination $archiveBundlePath -Force

$bundleHash = (Get-FileHash -Path $archiveBundlePath -Algorithm SHA256).Hash
$metadata = [ordered]@{
    applicationId = $applicationId
    versionCode = [int]$versionCode
    versionName = $versionName
    releaseTag = $releaseTag
    bundleFile = $archiveFileName
    sourceBundle = $bundleOutputPath
    sha256 = $bundleHash
    builtAtUtc = [DateTime]::UtcNow.ToString("o")
}
$metadata | ConvertTo-Json | Set-Content -Path $archiveMetadataPath -Encoding utf8

Write-Host ""
Write-Host "Release bundle created:"
Write-Host $bundleOutputPath
Write-Host ""
Write-Host "Versioned bundle archived in repo:"
Write-Host $archiveBundlePath
Write-Host ""
Write-Host "Release metadata:"
Write-Host $archiveMetadataPath
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Open Google Play Console."
Write-Host "2. Go to your BiteWise app."
Write-Host "3. Create or open an Internal, Closed, or Production release."
Write-Host "4. Upload the versioned .aab from the repo archive path shown above."
Write-Host "5. Save the release and continue through the Play Console checklist."
