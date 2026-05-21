param(
    [switch]$InstallDependencies,
    [switch]$WriteAndroidLocalProperties,
    [string]$AndroidSdkPath
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)

    Write-Host ""
    Write-Host "== $Title ==" -ForegroundColor Cyan
}

function Invoke-VersionCommand {
    param(
        [string]$Source,
        [string]$VersionArgument
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $Source $VersionArgument 2>&1
        $exitCode = $LASTEXITCODE

        return @{
            ExitCode = $exitCode
            Output = $output
        }
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Format-ExceptionMessage {
    param([System.Exception]$Exception)

    $message = [regex]::Replace($Exception.Message, "At [A-Z]:\\.*$", "", "Singleline")
    return $message.Trim()
}

function Test-Tool {
    param(
        [string]$Name,
        [string]$VersionArgument = "--version",
        [string]$InstallHint
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) {
        Write-Host "[missing] $Name" -ForegroundColor Yellow
        if ($InstallHint) {
            Write-Host "          $InstallHint"
        }
        return $false
    }

    try {
        $result = Invoke-VersionCommand $command.Source $VersionArgument
        $output = $result.Output | Select-Object -First 1
        if ($result.ExitCode -eq 0) {
            Write-Host "[ok]      $Name - $output" -ForegroundColor Green
            return $true
        }

        Write-Host "[broken]  $Name at $($command.Source)" -ForegroundColor Red
        Write-Host "          $output"
        if ($InstallHint) {
            Write-Host "          $InstallHint"
        }
        return $false
    }
    catch {
        Write-Host "[broken]  $Name at $($command.Source)" -ForegroundColor Red
        Write-Host "          $(Format-ExceptionMessage $_.Exception)"
        if ($InstallHint) {
            Write-Host "          $InstallHint"
        }
        return $false
    }
}

function Test-Java {
    $command = Get-Command java -ErrorAction SilentlyContinue
    if (-not $command) {
        Write-Host "[missing] java" -ForegroundColor Yellow
        Write-Host "          Install Android Studio or Temurin JDK 21."
        return $false
    }

    try {
        $result = Invoke-VersionCommand $command.Source "-version"
        $output = $result.Output | Select-Object -First 1
        if ($result.ExitCode -eq 0) {
            Write-Host "[ok]      java - $output" -ForegroundColor Green
            return $true
        }

        Write-Host "[broken]  java at $($command.Source)" -ForegroundColor Red
        Write-Host "          $output"
        return $false
    }
    catch {
        Write-Host "[broken]  java at $($command.Source)" -ForegroundColor Red
        Write-Host "          $(Format-ExceptionMessage $_.Exception)"
        return $false
    }
}

function Test-Terraform {
    $previousConfigFile = $env:TF_CLI_CONFIG_FILE

    if (-not $previousConfigFile) {
        $tempConfigFile = Join-Path $env:TEMP "terraform-empty.rc"
        if (-not (Test-Path $tempConfigFile)) {
            New-Item -ItemType File -Path $tempConfigFile | Out-Null
        }
        $env:TF_CLI_CONFIG_FILE = $tempConfigFile
    }

    try {
        return Test-Tool "terraform" "version" "Install Terraform with Scoop: scoop install terraform."
    }
    finally {
        $env:TF_CLI_CONFIG_FILE = $previousConfigFile
    }
}

function Resolve-AndroidSdkPath {
    param([string]$ExplicitPath)

    if ($ExplicitPath) {
        return $ExplicitPath
    }

    if ($env:ANDROID_HOME) {
        return $env:ANDROID_HOME
    }

    if ($env:ANDROID_SDK_ROOT) {
        return $env:ANDROID_SDK_ROOT
    }

    $defaultPath = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $defaultPath) {
        return $defaultPath
    }

    return $null
}

Write-Host "CalorieTracker Windows setup check" -ForegroundColor White
Write-Host "Repository: $PSScriptRoot\.."

Write-Section "Required tools"
$nodeOk = Test-Tool "node" "--version" "Install Node.js LTS from https://nodejs.org/ and reopen PowerShell."
$npmOk = Test-Tool "npm" "--version" "npm is installed with Node.js LTS. Reinstall Node.js if npm is missing."
$gitOk = Test-Tool "git" "--version" "Install Git for Windows from https://git-scm.com/download/win."
$javaOk = Test-Java

Write-Section "Infrastructure tools"
$terraformOk = Test-Terraform
$awsOk = Test-Tool "aws" "--version" "Install AWS CLI v2 from https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html."

Write-Section "Android SDK"
$resolvedAndroidSdkPath = Resolve-AndroidSdkPath $AndroidSdkPath
if ($resolvedAndroidSdkPath) {
    Write-Host "[ok]      Android SDK path candidate: $resolvedAndroidSdkPath" -ForegroundColor Green

    if ($WriteAndroidLocalProperties) {
        $localPropertiesPath = Join-Path $PSScriptRoot "..\apps\android\local.properties"
        $normalizedSdkPath = $resolvedAndroidSdkPath -replace "\\", "/"
        "sdk.dir=$normalizedSdkPath" | Set-Content -Path $localPropertiesPath -Encoding ascii
        Write-Host "[written] apps\android\local.properties"
    }
}
else {
    Write-Host "[missing] Android SDK path" -ForegroundColor Yellow
    Write-Host "          Install Android Studio, then set ANDROID_HOME or run with -AndroidSdkPath."
}

if ($InstallDependencies) {
    Write-Section "Workspace dependencies"
    if (-not ($nodeOk -and $npmOk)) {
        throw "Node.js and npm must be runnable before installing workspace dependencies."
    }

    $npmCommand = Get-Command "npm.cmd" -ErrorAction SilentlyContinue
    if ($npmCommand) {
        & $npmCommand.Source install
    }
    else {
        npm install
    }
}

Write-Section "Summary"
if ($nodeOk -and $npmOk -and $gitOk -and $javaOk) {
    Write-Host "Core development tools are ready." -ForegroundColor Green
}
else {
    Write-Host "Install or repair the missing core tools above, then rerun this script." -ForegroundColor Yellow
}

if (-not ($terraformOk -and $awsOk)) {
    Write-Host "Terraform and AWS CLI are only needed for deployed infrastructure work."
}

Write-Host ""
Write-Host "Next checks:"
Write-Host "  npm install"
Write-Host "  npm run typecheck"
Write-Host "  npm run dev --workspace @calorie-tracker/web"
