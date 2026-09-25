[CmdletBinding()]
param(
    [string]$ProjectRoot = "",
    [switch]$PlanOnly,
    [switch]$SkipTerraformInit
)

$ErrorActionPreference = "Stop"

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found on PATH."
    }
}

function Invoke-Checked([string]$Command, [string[]]$Arguments) {
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Command failed with exit code $LASTEXITCODE."
    }
}

Require-Command "terraform"
Require-Command "aws"
Require-Command "dotnet"

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = Split-Path -Parent $PSScriptRoot
}
$ProjectRoot = (Resolve-Path $ProjectRoot).Path
$terraformRoot = Join-Path $ProjectRoot "infra\terraform\environments\dev"
$variablesPath = Join-Path $terraformRoot "local.auto.tfvars"

if (-not (Test-Path (Join-Path $terraformRoot "backend.hcl"))) {
    throw "The dev Terraform backend configuration was not found at $terraformRoot."
}

$awsRegion = if ($env:AWS_REGION) { $env:AWS_REGION } else { "us-east-1" }
$appName = if ($env:TF_APP_NAME) { $env:TF_APP_NAME } else { "calorie-tracker" }
$cognitoDomainPrefix = if ($env:TF_COGNITO_DOMAIN_PREFIX) { $env:TF_COGNITO_DOMAIN_PREFIX } else { "" }
$webCallbackUrl = if ($env:TF_WEB_CALLBACK_URL) { $env:TF_WEB_CALLBACK_URL } else { "http://localhost:3000/auth/callback" }
$webLogoutUrl = if ($env:TF_WEB_LOGOUT_URL) { $env:TF_WEB_LOGOUT_URL } else { "http://localhost:3000/" }
$geminiSecretArn = if ($env:TF_GEMINI_API_SECRET_ARN) { $env:TF_GEMINI_API_SECRET_ARN } else { "" }

$lines = @(
    "aws_region = `"$awsRegion`"",
    "app_name = `"$appName`"",
    "environment = `"dev`"",
    "web_callback_urls = [`"$webCallbackUrl`"]",
    "web_logout_urls = [`"$webLogoutUrl`"]",
    "android_callback_urls = [`"bitewise-dev://auth/callback`", `"bitewise://auth/callback`"]",
    "android_logout_urls = [`"bitewise-dev://signout`", `"bitewise://signout`"]"
)
if ($cognitoDomainPrefix) { $lines += "cognito_domain_prefix = `"$cognitoDomainPrefix`"" }
if ($geminiSecretArn) { $lines += "gemini_api_secret_arn = `"$geminiSecretArn`"" }

Write-Host "Deploying the current working tree to the dev backend."
Write-Host "No commit or push is required."
Write-Host "Terraform environment: $terraformRoot"

$buildScript = Join-Path $ProjectRoot "scripts\build-api-dotnet-lambda.ps1"
& powershell -ExecutionPolicy Bypass -File $buildScript
if ($LASTEXITCODE -ne 0) { throw "Lambda package build failed." }

$lines | Set-Content -Path $variablesPath -Encoding utf8NoBOM
try {
    Push-Location $ProjectRoot
    if (-not $SkipTerraformInit) {
        Invoke-Checked "terraform" @("-chdir=$terraformRoot", "init", "-backend-config=backend.hcl")
    }

    Invoke-Checked "terraform" @("-chdir=$terraformRoot", "plan", "-var-file=local.auto.tfvars")
    if (-not $PlanOnly) {
        Invoke-Checked "terraform" @("-chdir=$terraformRoot", "apply", "-auto-approve", "-var-file=local.auto.tfvars")
        Write-Host "Dev backend deployment completed."
    } else {
        Write-Host "Plan completed; no resources were changed."
    }
}
finally {
    Pop-Location
    if (Test-Path $variablesPath) { Remove-Item -Force $variablesPath }
}
