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
$workspaceNodeModules = Join-Path $repoRoot "node_modules"
$apiDistRoot = Join-Path $apiRoot "dist"
$stagingRoot = Join-Path $apiRoot ".lambda-build\package"
$sharedPackageRoot = Join-Path $stagingRoot "node_modules\@calorie-tracker\shared"
$zodPackageRoot = Join-Path $stagingRoot "node_modules\zod"

Write-Host "Building shared and API workspaces..."
Push-Location $repoRoot
try {
    npm.cmd run build --workspace @calorie-tracker/shared
    npm.cmd run build --workspace @calorie-tracker/api
}
finally {
    Pop-Location
}

if (Test-Path $stagingRoot) {
    Remove-Item -Recurse -Force $stagingRoot
}

New-Item -ItemType Directory -Force -Path $stagingRoot | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $OutputPath) | Out-Null

Write-Host "Copying API dist files..."
Copy-Item -Recurse -Force (Join-Path $apiDistRoot "handlers") $stagingRoot
Copy-Item -Recurse -Force (Join-Path $apiDistRoot "lib") $stagingRoot

Write-Host "Vendoring zod..."
New-Item -ItemType Directory -Force -Path $zodPackageRoot | Out-Null
Copy-Item -Recurse -Force (Join-Path $workspaceNodeModules "zod\*") $zodPackageRoot

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
