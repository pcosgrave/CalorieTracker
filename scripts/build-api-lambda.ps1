param(
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..")

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $repoRoot "apps\api\dist\lambda\api.zip"
}

$apiRoot = Join-Path $repoRoot "apps\api"
$sharedRoot = Join-Path $repoRoot "shared"
$apiDistRoot = Join-Path $apiRoot "dist"
$buildRoot = Join-Path $apiRoot ".lambda-build"
$stagingRoot = Join-Path $buildRoot "package"
$sharedPackageRoot = Join-Path $stagingRoot "node_modules\@calorie-tracker\shared"
$apiPackageJsonPath = Join-Path $apiRoot "package.json"

Write-Host "Building shared and API workspaces..."
Push-Location $repoRoot
try {
    npm.cmd run build --workspace @calorie-tracker/shared
    npm.cmd run build --workspace @calorie-tracker/api
}
finally {
    Pop-Location
}

if (Test-Path $buildRoot) {
    Remove-Item -Recurse -Force $buildRoot
}

New-Item -ItemType Directory -Force -Path $stagingRoot | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $OutputPath) | Out-Null

Write-Host "Copying API dist files..."
Copy-Item -Recurse -Force (Join-Path $apiDistRoot "handlers") $stagingRoot
Copy-Item -Recurse -Force (Join-Path $apiDistRoot "lib") $stagingRoot

$apiPackage = Get-Content -Raw -Path $apiPackageJsonPath | ConvertFrom-Json
$runtimeDependencies = [ordered]@{}
foreach ($property in $apiPackage.dependencies.PSObject.Properties) {
    if ($property.Name -ne "@calorie-tracker/shared") {
        $runtimeDependencies[$property.Name] = $property.Value
    }
}

$runtimePackageJson = [ordered]@{
    name = "@calorie-tracker/api-lambda-runtime"
    private = $true
    type = "module"
    dependencies = $runtimeDependencies
}

$runtimePackageJsonPath = Join-Path $stagingRoot "package.json"
$runtimePackageJsonJson = $runtimePackageJson | ConvertTo-Json -Depth 10
[System.IO.File]::WriteAllText(
    $runtimePackageJsonPath,
    $runtimePackageJsonJson,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Installing runtime npm dependencies..."
Push-Location $stagingRoot
try {
    npm.cmd install --omit=dev --ignore-scripts
}
finally {
    Pop-Location
}

Write-Host "Vendoring shared package..."
New-Item -ItemType Directory -Force -Path $sharedPackageRoot | Out-Null
Copy-Item -Recurse -Force (Join-Path $sharedRoot "dist") $sharedPackageRoot
Copy-Item -Force (Join-Path $sharedRoot "package.json") $sharedPackageRoot

if (Test-Path $OutputPath) {
    Remove-Item -Force $OutputPath
}

Write-Host "Creating Lambda zip at $OutputPath..."
Compress-Archive -Path (Join-Path $stagingRoot "*") -DestinationPath $OutputPath -CompressionLevel Optimal

Write-Host "Lambda package ready: $OutputPath"
