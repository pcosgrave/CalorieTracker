[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Name,

    [string]$BaseBranch = "main",
    [string]$WorktreeRoot = ""
)

$ErrorActionPreference = "Stop"

function Invoke-Git([string[]]$Arguments) {
    $output = & git @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw (($output | Out-String).Trim())
    }
    return $output
}

$repoRoot = (Invoke-Git @("rev-parse", "--show-toplevel") | Select-Object -First 1).Trim()
$repoRoot = [IO.Path]::GetFullPath($repoRoot)
$safeName = ($Name.Trim().ToLowerInvariant() -replace "[^a-z0-9]+", "-").Trim("-")
if ([string]::IsNullOrWhiteSpace($safeName)) { throw "Feature name must contain at least one letter or number." }

$branch = "codex/$safeName"
if ([string]::IsNullOrWhiteSpace($WorktreeRoot)) {
    $WorktreeRoot = Join-Path (Split-Path $repoRoot -Parent) "CalorieTracker-worktrees"
}
$worktreePath = Join-Path $WorktreeRoot $safeName

$worktrees = @(Invoke-Git @("worktree", "list", "--porcelain")) -join "`n"
$branchLine = "branch refs/heads/$branch"
$existingBranchWorktree = $null
$blocks = $worktrees -split "`n`n"
foreach ($block in $blocks) {
    if ($block -match [regex]::Escape($branchLine)) {
        $existingBranchWorktree = (($block -split "`n") | Where-Object { $_ -like "worktree *" } | Select-Object -First 1) -replace "^worktree ", ""
        break
    }
}
if ($existingBranchWorktree) {
    $commit = (Invoke-Git @("-C", $existingBranchWorktree, "rev-parse", "HEAD") | Select-Object -First 1).Trim()
    Write-Output "EXISTING"
    Write-Output "branch=$branch"
    Write-Output "worktree=$existingBranchWorktree"
    Write-Output "startingCommit=$commit"
    exit 0
}

$baseRef = (Invoke-Git @("show-ref", "--verify", "refs/heads/$BaseBranch") | Select-Object -First 1)
if (-not $baseRef) { throw "Local base branch '$BaseBranch' does not exist." }

if ((Invoke-Git @("status", "--porcelain"))) {
    Write-Warning "The base checkout has uncommitted changes. They will not be copied into the new worktree."
}

if (Test-Path -LiteralPath $worktreePath) {
    throw "Worktree path already exists: $worktreePath"
}

New-Item -ItemType Directory -Force -Path $WorktreeRoot | Out-Null
Invoke-Git @("worktree", "add", "-b", $branch, $worktreePath, $BaseBranch) | Out-Null
$commit = (Invoke-Git @("-C", $worktreePath, "rev-parse", "HEAD") | Select-Object -First 1).Trim()

Write-Output "CREATED"
Write-Output "branch=$branch"
Write-Output "worktree=$worktreePath"
Write-Output "startingCommit=$commit"
Write-Output "baseBranch=$BaseBranch"
