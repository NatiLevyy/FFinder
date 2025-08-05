# FFinder Code Formatting Script (PowerShell)
# Applies Kotlin best practices with Jetpack Compose support

param(
    [switch]$Check,
    [switch]$Fix,
    [switch]$Detekt,
    [switch]$All,
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
    Write-ColorOutput $Blue "         FFinder Code Formatting Tool        "
    Write-ColorOutput $Blue "=============================================="
    Write-Output ""
}

function Show-Usage {
    Write-Output "Usage: .\format-code.ps1 [OPTIONS]"
    Write-Output ""
    Write-Output "Options:"
    Write-Output "  -Check     Run ktlint check only (no formatting)"
    Write-Output "  -Fix       Run ktlint format to fix issues"
    Write-Output "  -Detekt    Run detekt static analysis"
    Write-Output "  -All       Run all checks and formatting"
    Write-Output "  -Verbose   Show detailed output"
    Write-Output ""
    Write-Output "Examples:"
    Write-Output "  .\format-code.ps1 -All          # Run all tools"
    Write-Output "  .\format-code.ps1 -Fix          # Format code only"
    Write-Output "  .\format-code.ps1 -Check        # Check style only"
    Write-Output "  .\format-code.ps1 -Detekt       # Run static analysis"
}

function Test-GradleWrapper {
    if (Test-Path ".\gradlew.bat") {
        return $true
    } else {
        Write-ColorOutput $Red "Error: gradlew.bat not found in current directory"
        Write-ColorOutput $Red "Please run this script from the android/ directory"
        return $false
    }
}

function Run-KtlintCheck {
    Write-ColorOutput $Blue "Running ktlint check..."
    Write-Output ""
    
    $result = & .\gradlew.bat ktlintCheck
    $exitCode = $LASTEXITCODE
    
    if ($exitCode -eq 0) {
        Write-ColorOutput $Green "✅ Ktlint check passed!"
    } else {
        Write-ColorOutput $Red "❌ Ktlint check failed!"
        Write-ColorOutput $Yellow "Run with -Fix to automatically fix formatting issues"
    }
    
    return $exitCode
}

function Run-KtlintFormat {
    Write-ColorOutput $Blue "Running ktlint format..."
    Write-Output ""
    
    $result = & .\gradlew.bat ktlintFormat
    $exitCode = $LASTEXITCODE
    
    if ($exitCode -eq 0) {
        Write-ColorOutput $Green "✅ Code formatted successfully!"
    } else {
        Write-ColorOutput $Red "❌ Formatting failed!"
    }
    
    return $exitCode
}

function Run-Detekt {
    Write-ColorOutput $Blue "Running detekt static analysis..."
    Write-Output ""
    
    $result = & .\gradlew.bat detekt
    $exitCode = $LASTEXITCODE
    
    if ($exitCode -eq 0) {
        Write-ColorOutput $Green "✅ Detekt analysis passed!"
    } else {
        Write-ColorOutput $Red "❌ Detekt analysis found issues!"
        
        # Check if HTML report exists
        $reportPath = "app\build\reports\detekt\detekt.html"
        if (Test-Path $reportPath) {
            Write-ColorOutput $Yellow "📊 Opening detekt report..."
            Start-Process $reportPath
        }
    }
    
    return $exitCode
}

function Run-BuildCheck {
    Write-ColorOutput $Blue "Verifying build after formatting..."
    Write-Output ""
    
    $result = & .\gradlew.bat compileDebugKotlin
    $exitCode = $LASTEXITCODE
    
    if ($exitCode -eq 0) {
        Write-ColorOutput $Green "✅ Build verification passed!"
    } else {
        Write-ColorOutput $Red "❌ Build verification failed!"
    }
    
    return $exitCode
}

function Show-Summary($results) {
    Write-Output ""
    Write-ColorOutput $Blue "=============================================="
    Write-ColorOutput $Blue "                 SUMMARY                      "
    Write-ColorOutput $Blue "=============================================="
    
    $allPassed = $true
    
    foreach ($result in $results) {
        if ($result.ExitCode -eq 0) {
            Write-ColorOutput $Green "✅ $($result.Name): PASSED"
        } else {
            Write-ColorOutput $Red "❌ $($result.Name): FAILED"
            $allPassed = $false
        }
    }
    
    Write-Output ""
    if ($allPassed) {
        Write-ColorOutput $Green "🎉 All checks passed! Your code follows FFinder standards."
    } else {
        Write-ColorOutput $Red "⚠️  Some checks failed. Please review and fix the issues above."
    }
    
    Write-Output ""
}

# Main execution
Show-Header

# Check if no parameters provided
if (-not ($Check -or $Fix -or $Detekt -or $All)) {
    Show-Usage
    exit 0
}

# Verify gradle wrapper exists
if (-not (Test-GradleWrapper)) {
    exit 1
}

$results = @()

try {
    if ($All) {
        # Run all tools in sequence
        Write-ColorOutput $Yellow "Running complete FFinder code quality check..."
        Write-Output ""
        
        # 1. Format code first
        $formatResult = Run-KtlintFormat
        $results += @{ Name = "Ktlint Format"; ExitCode = $formatResult }
        
        # 2. Check formatting
        $checkResult = Run-KtlintCheck
        $results += @{ Name = "Ktlint Check"; ExitCode = $checkResult }
        
        # 3. Run static analysis
        $detektResult = Run-Detekt
        $results += @{ Name = "Detekt Analysis"; ExitCode = $detektResult }
        
        # 4. Verify build
        $buildResult = Run-BuildCheck
        $results += @{ Name = "Build Verification"; ExitCode = $buildResult }
        
    } elseif ($Fix) {
        $formatResult = Run-KtlintFormat
        $results += @{ Name = "Ktlint Format"; ExitCode = $formatResult }
        
        # Also run check after formatting
        $checkResult = Run-KtlintCheck
        $results += @{ Name = "Ktlint Check"; ExitCode = $checkResult }
        
    } elseif ($Check) {
        $checkResult = Run-KtlintCheck
        $results += @{ Name = "Ktlint Check"; ExitCode = $checkResult }
        
    } elseif ($Detekt) {
        $detektResult = Run-Detekt
        $results += @{ Name = "Detekt Analysis"; ExitCode = $detektResult }
    }
    
    Show-Summary $results
    
    # Exit with error if any check failed
    $hasFailures = $results | Where-Object { $_.ExitCode -ne 0 }
    if ($hasFailures) {
        exit 1
    } else {
        exit 0
    }
    
} catch {
    Write-ColorOutput $Red "Error occurred during execution: $($_.Exception.Message)"
    exit 1
}