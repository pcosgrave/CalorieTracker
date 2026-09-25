param(
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..")

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $repoRoot "apps\api-dotnet\dist\lambda\api.zip"
}

$projectRoot = Join-Path $repoRoot "apps\api-dotnet\src\CalorieTracker.Api"
$buildRoot = Join-Path $repoRoot "apps\api-dotnet\.lambda-build"
$publishRoot = Join-Path $buildRoot "publish"
$nugetConfigPath = Join-Path $repoRoot "apps\api-dotnet\NuGet.Config"

if (Test-Path $buildRoot) {
    Remove-Item -Recurse -Force $buildRoot
}

New-Item -ItemType Directory -Force -Path $publishRoot | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $OutputPath) | Out-Null

Write-Host "Publishing .NET Lambda app..."
Push-Location $projectRoot
try {
    & dotnet publish .\CalorieTracker.Api.csproj `
        -c Release `
        -o $publishRoot `
        --configfile $nugetConfigPath `
        /p:UseAppHost=false `
        /p:GenerateRuntimeConfigurationFiles=true

    if ($LASTEXITCODE -ne 0) {
        throw "dotnet publish failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

if (Test-Path $OutputPath) {
    Remove-Item -Force $OutputPath
}

Write-Host "Creating Lambda zip at $OutputPath..."
Compress-Archive -Path (Join-Path $publishRoot "*") -DestinationPath $OutputPath -CompressionLevel Optimal

Write-Host "Lambda package ready: $OutputPath"
