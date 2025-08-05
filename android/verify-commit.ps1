# FFinder Commit Policy Verification Script (PowerShell)
# Verifies commit message format, lint checks, and unit tests

param(
    [string]$CommitMessage,
    [switch]$SkipTests,
    [switch]$SkipLint,
    [switch]$Verbose
)

# Colors for output
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Cyan"

function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    } else {
        $input | Write-Output
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Show-Header {
    Write-ColorOutput $Blue "=============================================="
    Write-ColorOutput $Blue "         FFinder Commit Policy Verifier        "
    Write-ColorOutput $Blue "=============================================="
    Write-Output ""
}

function Show-Usage {
    Write-Output "Usage: .\verify-commit.ps1 [OPTIONS] [COMMIT_MESSAGE]"
    Write-Output ""
    Write-Output "Options:"
    Write-Output "  -CommitMessage ""message""  Commit message to verify"
    Write-Output "  -SkipTests              Skip unit test verification"
    Write-Output "  -SkipLint               Skip lint check verification"
    Write-Output "  -SkipSigning            Skip commit signing verification"
    Write-Output "  -Verbose                Show detailed output"
    Write-Output ""
    Write-Output "Examples:"
    Write-Output "  .\verify-commit.ps1 -CommitMessage ""[Feature] Add location sharing"""
    Write-Output "  .\verify-commit.ps1 -SkipTests -CommitMessage ""[Fix] Update README"""
}

function Test-CommitMessageFormat {
    param(
        [string]$Message
    )
    
    Write-ColorOutput $Blue "Checking commit message format..."
    
    # Valid commit types
    $validTypes = @("Feature", "Fix", "Refactor", "Style", "Docs", "Test", "Chore", "Perf")
    
    # Check if message follows [Type] Description format
    $pattern = "^\[($($validTypes -join '|'))\]\s+.+$"
    
    if ($Message -match $pattern) {
        Write-ColorOutput $Green "✅ Commit message format is valid!"
        return $true
    } else {
        Write-ColorOutput $Red "❌ Invalid commit message format!"
        Write-ColorOutput $Yellow "Commit message must follow format: [Type] Description"
        Write-ColorOutput $Yellow "Valid types: $($validTypes -join ', ')"
        Write-ColorOutput $Yellow "Example: [Feature] Add animated FAB for location sharing"
        return $false
    }
}

function Test-LintChecks {
    Write-ColorOutput $Blue "Running lint checks..."
    
    # Run format-code.ps1 with -Check flag
    if (Test-Path ".\format-code.ps1") {
        $result = & .\format-code.ps1 -Check
        $lintExitCode = $LASTEXITCODE
        
        if ($lintExitCode -eq 0) {
            Write-ColorOutput $Green "✅ Lint checks passed!"
            return $true
        } else {
            Write-ColorOutput $Red "❌ Lint checks failed!"
            Write-ColorOutput $Yellow "Run .\format-code.ps1 -Fix to fix formatting issues"
            return $false
        }
    } else {
        Write-ColorOutput $Red "❌ format-code.ps1 not found!"
        Write-ColorOutput $Yellow "Make sure you're running this script from the android directory"
        return $false
    }
}

function Test-UnitTests {
    Write-ColorOutput $Blue "Running unit tests..."
    
    # Run unit tests using gradlew
    if (Test-Path ".\gradlew.bat") {
        $result = & .\gradlew.bat testDebugUnitTest
        $testExitCode = $LASTEXITCODE
        
        if ($testExitCode -eq 0) {
            Write-ColorOutput $Green "✅ Unit tests passed!"
            return $true
        } else {
            Write-ColorOutput $Red "❌ Unit tests failed!"
            Write-ColorOutput $Yellow "Fix failing tests before committing"
            return $false
        }
    } else {
        Write-ColorOutput $Red "❌ gradlew.bat not found!"
        Write-ColorOutput $Yellow "Make sure you're running this script from the android directory"
        return $false
    }
}

# Function removed as commit signing is no longer required

function Show-Summary($results) {
    Write-Output ""
    Write-ColorOutput $Blue "=============================================="
    Write-ColorOutput $Blue "                 SUMMARY                      "
    Write-ColorOutput $Blue "=============================================="
    
    $allPassed = $true
    
    foreach ($result in $results) {
        if ($result.Result) {
            Write-ColorOutput $Green "✅ $($result.Name): PASSED"
        } else {
            Write-ColorOutput $Red "❌ $($result.Name): FAILED"
            $allPassed = $false
        }
    }
    
    Write-Output ""
    if ($allPassed) {
        Write-ColorOutput $Green "🎉 All checks passed! Your commit meets FFinder policy requirements."
    } else {
        Write-ColorOutput $Red "⚠️  Some checks failed. Please fix the issues before committing."
    }
    
    Write-Output ""
    
    return $allPassed
}

# Main execution
Show-Header

# Check if no parameters provided
if (-not $CommitMessage -and -not $SkipTests -and -not $SkipLint -and -not $SkipSigning) {
    Show-Usage
    exit 0
}

$results = @()

# Check commit message format if provided
if ($CommitMessage) {
    $messageResult = Test-CommitMessageFormat -Message $CommitMessage
    $results += @{ Name = "Commit Message Format"; Result = $messageResult }
}

# Check lint unless skipped
if (-not $SkipLint) {
    $lintResult = Test-LintChecks
    $results += @{ Name = "Lint Checks"; Result = $lintResult }
}

# Check unit tests unless skipped
if (-not $SkipTests) {
    $testsResult = Test-UnitTests
    $results += @{ Name = "Unit Tests"; Result = $testsResult }
}

# Commit signing check removed as it's no longer required

# Show summary and exit with appropriate code
$allPassed = Show-Summary $results

if ($allPassed) {
    exit 0
} else {
    exit 1
}