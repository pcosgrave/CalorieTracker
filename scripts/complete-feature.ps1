[CmdletBinding()]
param(
    [string]$Message = "Complete feature",
    [switch]$AllowNoTests,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Invoke-Git([string[]]$Arguments) {
    $output = & git @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw (($output | Out-String).Trim()) }
    return $output
}

$repoRoot = (Invoke-Git @("rev-parse", "--show-toplevel") | Select-Object -First 1).Trim()
$branch = (Invoke-Git @("branch", "--show-current") | Select-Object -First 1).Trim()
if ([string]::IsNullOrWhiteSpace($branch) -or $branch -eq "main") {
    throw "Complete a feature from its feature worktree, not from main or a detached checkout."
}
if ($branch -notlike "codex/*") { throw "Feature branches must use the codex/<name> convention." }

$status = @(Invoke-Git @("status", "--porcelain"))
if (-not $status) { throw "There are no changes to complete." }

$base = (Invoke-Git @("merge-base", "HEAD", "main") | Select-Object -First 1).Trim()
$changed = @(Invoke-Git @("diff", "--name-only", "$base..HEAD"))
$stagedAndUntracked = $status | ForEach-Object { ($_ -replace "^...", "").Trim() }
$allChanged = @($changed + $stagedAndUntracked) | Where-Object { $_ }
$hasSourceChanges = $allChanged | Where-Object { $_ -notmatch "(^|/)(test|tests|__tests__|ui-checks)(/|$)|\.(test|spec)\.[^.]+$|Test\.[^.]+$" }
$hasTestChanges = $allChanged | Where-Object { $_ -match "(^|/)(test|tests|__tests__|ui-checks)(/|$)|\.(test|spec)\.[^.]+$|Test\.[^.]+$" }
if ($hasSourceChanges -and -not $hasTestChanges -and -not $AllowNoTests) {
    throw "Behavior changes require added or updated tests. Use -AllowNoTests only for a documented non-behavioral change."
}

if ($DryRun) {
    Write-Output "DRY-RUN"
    Write-Output "branch=$branch"
    Write-Output "checks=not-run"
    Write-Output "files=$($allChanged -join ',')"
    exit 0
}

Push-Location $repoRoot
try {
    & npm run test
    if ($LASTEXITCODE -ne 0) { throw "Tests failed." }
    & npm run typecheck
    if ($LASTEXITCODE -ne 0) { throw "Typecheck failed." }
    & npm run lint
    if ($LASTEXITCODE -ne 0) { throw "Lint failed." }

    Invoke-Git @("add", "-A") | Out-Null
    & git diff --cached --quiet
    if ($LASTEXITCODE -eq 0) { throw "Nothing is staged after validation." }
    Invoke-Git @("commit", "-m", $Message) | Out-Null
    Invoke-Git @("push", "--set-upstream", "origin", $branch) | Out-Null
} finally { Pop-Location }

$gh = Get-Command gh -ErrorAction SilentlyContinue
if (-not $gh) {
    Write-Warning "GitHub CLI (gh) is not installed; branch was pushed but PR was not opened."
    Write-Output "PR_COMMAND=gh pr create --base main --head $branch --fill"
    exit 2
}
& gh pr create --base main --head $branch --fill
if ($LASTEXITCODE -ne 0) { throw "Branch was pushed, but PR creation failed." }
