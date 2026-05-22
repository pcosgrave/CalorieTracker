param(
    [Parameter(Mandatory = $true)]
    [string]$BucketName,

    [Parameter(Mandatory = $true)]
    [string]$LockTableName,

    [string]$Region = "ca-central-1"
)

$ErrorActionPreference = "Stop"

function Test-AwsCli {
    $null = Get-Command aws -ErrorAction Stop
}

function Test-S3BucketExists {
    param([string]$Name)

    try {
        $null = aws s3api head-bucket --bucket $Name 2>$null
        return $LASTEXITCODE -eq 0
    }
    catch {
        return $false
    }
}

function Test-DynamoTableExists {
    param(
        [string]$Name,
        [string]$AwsRegion
    )

    try {
        $null = aws dynamodb describe-table --table-name $Name --region $AwsRegion 2>$null
        return $LASTEXITCODE -eq 0
    }
    catch {
        return $false
    }
}

Test-AwsCli

Write-Host "Bootstrapping Terraform backend resources in $Region..."

if (-not (Test-S3BucketExists -Name $BucketName)) {
    Write-Host "Creating S3 bucket $BucketName..."

    if ($Region -eq "us-east-1") {
        aws s3api create-bucket --bucket $BucketName --region $Region | Out-Null
    }
    else {
        aws s3api create-bucket --bucket $BucketName --region $Region --create-bucket-configuration LocationConstraint=$Region | Out-Null
    }
}
else {
    Write-Host "S3 bucket $BucketName already exists."
}

Write-Host "Enabling S3 bucket versioning..."
aws s3api put-bucket-versioning --bucket $BucketName --versioning-configuration Status=Enabled --region $Region | Out-Null

Write-Host "Enabling S3 bucket server-side encryption..."
$encryptionConfiguration = @{
    Rules = @(
        @{
            ApplyServerSideEncryptionByDefault = @{
                SSEAlgorithm = "AES256"
            }
        }
    )
} | ConvertTo-Json -Depth 5 -Compress

$encryptionConfigPath = Join-Path ([System.IO.Path]::GetTempPath()) "calorie-tracker-tf-backend-encryption.json"
[System.IO.File]::WriteAllText(
    $encryptionConfigPath,
    $encryptionConfiguration,
    (New-Object System.Text.UTF8Encoding($false))
)

try {
    aws s3api put-bucket-encryption --bucket $BucketName --region $Region --server-side-encryption-configuration "file://$encryptionConfigPath" | Out-Null
}
finally {
    if (Test-Path $encryptionConfigPath) {
        Remove-Item -Force $encryptionConfigPath
    }
}

if (-not (Test-DynamoTableExists -Name $LockTableName -AwsRegion $Region)) {
    Write-Host "Creating DynamoDB lock table $LockTableName..."
    aws dynamodb create-table `
        --table-name $LockTableName `
        --attribute-definitions AttributeName=LockID,AttributeType=S `
        --key-schema AttributeName=LockID,KeyType=HASH `
        --billing-mode PAY_PER_REQUEST `
        --region $Region | Out-Null

    Write-Host "Waiting for DynamoDB lock table to become active..."
    aws dynamodb wait table-exists --table-name $LockTableName --region $Region
}
else {
    Write-Host "DynamoDB lock table $LockTableName already exists."
}

Write-Host "Terraform backend bootstrap complete."
