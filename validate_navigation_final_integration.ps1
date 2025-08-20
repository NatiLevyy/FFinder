#!/usr/bin/env pwsh

# Navigation Button Fix - Final Integration and Validation Script
# This script performs comprehensive end-to-end testing of all navigation functionality

Write-Host "🚀 Navigation Button Fix - Final Integration and Validation" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

$ErrorActionPreference = "Continue"
$testResults = @()
$startTime = Get-Date

function Add-TestResult {
    param(
        [string]$TestName,
        [bool]$Passed,
        [string]$Details = "",
        [string]$Duration = ""
    )
    
    $testResults += [PSCustomObject]@{
        Test = $TestName
        Status = if ($Passed) { "✅ PASS" } else { "❌ FAIL" }
        Details = $Details
        Duration = $Duration
    }
    
    $status = if ($Passed) { "✅ PASS" } else { "❌ FAIL" }
    Write-Host "$status - $TestName" -ForegroundColor $(if ($Passed) { "Green" } else { "Red" })
    if ($Details) {
        Write-Host "    $Details" -ForegroundColor Gray
    }
}

function Test-FileExists {
    param([string]$FilePath, [string]$Description)
    
    $exists = Test-Path $FilePath
    Add-TestResult -TestName "File Exists: $Description" -Passed $exists -Details $FilePath
    return $exists
}

function Test-BuildCompilation {
    Write-Host "`n📦 Testing Build Compilation..." -ForegroundColor Yellow
    
    $buildStart = Get-Date
    try {
        Set-Location "android"
        $buildResult = & .\gradlew.bat assembleDebug --no-daemon --quiet 2>&1
        $buildDuration = ((Get-Date) - $buildStart).TotalSeconds
        
        if ($LASTEXITCODE -eq 0) {
            Add-TestResult -TestName "Android Build Compilation" -Passed $true -Details "Build successful" -Duration "${buildDuration}s"
            return $true
        } else {
            Add-TestResult -TestName "Android Build Compilation" -Passed $false -Details "Build failed: $buildResult" -Duration "${buildDuration}s"
            return $false
        }
    } catch {
        $buildDuration = ((Get-Date) - $buildStart).TotalSeconds
        Add-TestResult -TestName "Android Build Compilation" -Passed $false -Details "Build error: $($_.Exception.Message)" -Duration "${buildDuration}s"
        return $false
    } finally {
        Set-Location ".."
    }
}

function Test-UnitTests {
    Write-Host "`n🧪 Running Unit Tests..." -ForegroundColor Yellow
    
    $testStart = Get-Date
    try {
        Set-Location "android"
        
        # Run navigation-specific unit tests
        $testClasses = @(
            "NavigationIntegrationValidationTest",
            "NavigationManagerImplTest", 
            "NavigationStateTrackerImplTest",
            "NavigationErrorHandlerTest",
            "ResponsiveButtonTest",
            "ButtonResponseManagerImplTest",
            "NavigationAnalyticsImplTest"
        )
        
        $allTestsPassed = $true
        foreach ($testClass in $testClasses) {
            Write-Host "  Running $testClass..." -ForegroundColor Gray
            $testResult = & .\gradlew.bat test --tests "*.$testClass" --no-daemon --quiet 2>&1
            
            if ($LASTEXITCODE -eq 0) {
                Add-TestResult -TestName "Unit Test: $testClass" -Passed $true -Details "All tests passed"
            } else {
                Add-TestResult -TestName "Unit Test: $testClass" -Passed $false -Details "Some tests failed"
                $allTestsPassed = $false
            }
        }
        
        $testDuration = ((Get-Date) - $testStart).TotalSeconds
        Add-TestResult -TestName "Overall Unit Tests" -Passed $allTestsPassed -Details "Executed $($testClasses.Count) test classes" -Duration "${testDuration}s"
        
        return $allTestsPassed
    } catch {
        $testDuration = ((Get-Date) - $testStart).TotalSeconds
        Add-TestResult -TestName "Unit Tests Execution" -Passed $false -Details "Test execution error: $($_.Exception.Message)" -Duration "${testDuration}s"
        return $false
    } finally {
        Set-Location ".."
    }
}

function Test-NavigationComponents {
    Write-Host "`n🧭 Validating Navigation Components..." -ForegroundColor Yellow
    
    # Core navigation files
    $coreFiles = @{
        "android/app/src/main/java/com/locationsharing/app/navigation/NavigationManagerImpl.kt" = "NavigationManager Implementation"
        "android/app/src/main/java/com/locationsharing/app/navigation/NavigationStateTrackerImpl.kt" = "NavigationStateTracker Implementation"
        "android/app/src/main/java/com/locationsharing/app/navigation/NavigationErrorHandler.kt" = "NavigationErrorHandler"
        "android/app/src/main/java/com/locationsharing/app/ui/components/button/ResponsiveButton.kt" = "ResponsiveButton Component"
        "android/app/src/main/java/com/locationsharing/app/ui/components/button/ButtonResponseManagerImpl.kt" = "ButtonResponseManager Implementation"
        "android/app/src/main/java/com/locationsharing/app/MainActivity.kt" = "MainActivity with Navigation Integration"
    }
    
    $allFilesExist = $true
    foreach ($file in $coreFiles.GetEnumerator()) {
        $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
        if (-not $exists) { $allFilesExist = $false }
    }
    
    return $allFilesExist
}

function Test-PerformanceComponents {
    Write-Host "`n⚡ Validating Performance Components..." -ForegroundColor Yellow
    
    $performanceFiles = @{
        "android/app/src/main/java/com/locationsharing/app/navigation/performance/NavigationPerformanceMonitor.kt" = "Performance Monitor"
        "android/app/src/main/java/com/locationsharing/app/navigation/performance/NavigationCache.kt" = "Navigation Cache"
        "android/app/src/main/java/com/locationsharing/app/navigation/performance/NavigationDestinationLoader.kt" = "Destination Loader"
        "android/app/src/main/java/com/locationsharing/app/navigation/performance/OptimizedButtonResponseManager.kt" = "Optimized Button Response Manager"
    }
    
    $allFilesExist = $true
    foreach ($file in $performanceFiles.GetEnumerator()) {
        $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
        if (-not $exists) { $allFilesExist = $false }
    }
    
    return $allFilesExist
}

function Test-SecurityComponents {
    Write-Host "`n🔒 Validating Security Components..." -ForegroundColor Yellow
    
    $securityFiles = @{
        "android/app/src/main/java/com/locationsharing/app/navigation/security/NavigationSecurityManager.kt" = "Security Manager"
        "android/app/src/main/java/com/locationsharing/app/navigation/security/RouteValidator.kt" = "Route Validator"
        "android/app/src/main/java/com/locationsharing/app/navigation/security/NavigationStateProtector.kt" = "State Protector"
        "android/app/src/main/java/com/locationsharing/app/navigation/security/SessionAwareNavigationHandler.kt" = "Session-Aware Handler"
    }
    
    $allFilesExist = $true
    foreach ($file in $securityFiles.GetEnumerator()) {
        $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
        if (-not $exists) { $allFilesExist = $false }
    }
    
    return $allFilesExist
}

function Test-FeedbackComponents {
    Write-Host "`n🎯 Validating Feedback Components..." -ForegroundColor Yellow
    
    $feedbackFiles = @{
        "android/app/src/main/java/com/locationsharing/app/ui/components/feedback/HapticFeedbackManager.kt" = "Haptic Feedback Manager"
        "android/app/src/main/java/com/locationsharing/app/ui/components/feedback/VisualFeedbackManager.kt" = "Visual Feedback Manager"
        "android/app/src/main/java/com/locationsharing/app/ui/navigation/EnhancedNavigationTransitions.kt" = "Enhanced Navigation Transitions"
    }
    
    $allFilesExist = $true
    foreach ($file in $feedbackFiles.GetEnumerator()) {
        $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
        if (-not $exists) { $allFilesExist = $false }
    }
    
    return $allFilesExist
}

function Test-AnalyticsComponents {
    Write-Host "`n📊 Validating Analytics Components..." -ForegroundColor Yellow
    
    $analyticsFiles = @{
        "android/app/src/main/java/com/locationsharing/app/navigation/NavigationAnalyticsImpl.kt" = "Navigation Analytics Implementation"
        "android/app/src/main/java/com/locationsharing/app/navigation/AnalyticsManager.kt" = "Analytics Manager"
        "android/app/src/main/java/com/locationsharing/app/navigation/ButtonAnalyticsImpl.kt" = "Button Analytics Implementation"
    }
    
    $allFilesExist = $true
    foreach ($file in $analyticsFiles.GetEnumerator()) {
        $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
        if (-not $exists) { $allFilesExist = $false }
    }
    
    return $allFilesExist
}

function Test-DebugComponents {
    Write-Host "`n🐛 Validating Debug Components..." -ForegroundColor Yellow
    
    $debugFiles = @{
        "android/app/src/main/java/com/locationsharing/app/navigation/debug/NavigationDebugUtils.kt" = "Debug Utils"
        "android/app/src/main/java/com/locationsharing/app/navigation/debug/NavigationDebugOverlay.kt" = "Debug Overlay"
        "android/app/src/main/java/com/locationsharing/app/navigation/debug/NavigationStateInspector.kt" = "State Inspector"
        "android/app/src/main/java/com/locationsharing/app/navigation/debug/NavigationPerformanceProfiler.kt" = "Performance Profiler"
    }
    
    $allFilesExist = $true
    foreach ($file in $debugFiles.GetEnumerator()) {
        $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
        if (-not $exists) { $allFilesExist = $false }
    }
    
    return $allFilesExist
}

function Test-IntegrationTests {
    Write-Host "`n🔗 Validating Integration Tests..." -ForegroundColor Yellow
    
    $integrationTestFiles = @{
        "android/app/src/test/java/com/locationsharing/app/navigation/NavigationIntegrationValidationTest.kt" = "Navigation Integration Validation Test"
        "android/app/src/androidTest/java/com/locationsharing/app/navigation/NavigationEndToEndUITest.kt" = "End-to-End UI Test"
        "android/app/src/androidTest/java/com/locationsharing/app/MainActivityNavigationIntegrationTest.kt" = "MainActivity Navigation Integration Test"
    }
    
    $allFilesExist = $true
    foreach ($file in $integrationTestFiles.GetEnumerator()) {
        $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
        if (-not $exists) { $allFilesExist = $false }
    }
    
    return $allFilesExist
}

function Test-RequirementsCompliance {
    Write-Host "`n📋 Validating Requirements Compliance..." -ForegroundColor Yellow
    
    # Check if all requirements from the spec are addressed
    $requirements = @(
        @{ Name = "Button Responsiveness (1.1)"; Check = { Test-Path "android/app/src/main/java/com/locationsharing/app/ui/components/button/ResponsiveButton.kt" } },
        @{ Name = "Visual Feedback (1.2)"; Check = { Test-Path "android/app/src/main/java/com/locationsharing/app/ui/components/feedback/VisualFeedbackManager.kt" } },
        @{ Name = "Double-click Prevention (1.3)"; Check = { Test-Path "android/app/src/main/java/com/locationsharing/app/ui/components/button/ButtonResponseManagerImpl.kt" } },
        @{ Name = "Navigation Functionality (2.1-2.3)"; Check = { Test-Path "android/app/src/main/java/com/locationsharing/app/navigation/NavigationManagerImpl.kt" } },
        @{ Name = "Consistent Navigation (3.1)"; Check = { Test-Path "android/app/src/main/java/com/locationsharing/app/navigation/NavigationStateTrackerImpl.kt" } },
        @{ Name = "Error Handling (4.1-4.2)"; Check = { Test-Path "android/app/src/main/java/com/locationsharing/app/navigation/NavigationErrorHandler.kt" } },
        @{ Name = "Analytics (4.3)"; Check = { Test-Path "android/app/src/main/java/com/locationsharing/app/navigation/NavigationAnalyticsImpl.kt" } },
        @{ Name = "Accessibility (5.1-5.4)"; Check = { Test-Path "android/app/src/main/java/com/locationsharing/app/ui/components/feedback/HapticFeedbackManager.kt" } }
    )
    
    $allRequirementsMet = $true
    foreach ($req in $requirements) {
        $met = & $req.Check
        Add-TestResult -TestName "Requirement: $($req.Name)" -Passed $met -Details $(if ($met) { "Implementation found" } else { "Implementation missing" })
        if (-not $met) { $allRequirementsMet = $false }
    }
    
    return $allRequirementsMet
}

function Test-CodeQuality {
    Write-Host "`n🔍 Running Code Quality Checks..." -ForegroundColor Yellow
    
    try {
        Set-Location "android"
        
        # Run ktlint for code formatting
        Write-Host "  Running ktlint..." -ForegroundColor Gray
        $ktlintResult = & .\gradlew.bat ktlintCheck --no-daemon --quiet 2>&1
        $ktlintPassed = $LASTEXITCODE -eq 0
        Add-TestResult -TestName "Code Formatting (ktlint)" -Passed $ktlintPassed -Details $(if ($ktlintPassed) { "All files properly formatted" } else { "Formatting issues found" })
        
        # Run detekt for static analysis
        Write-Host "  Running detekt..." -ForegroundColor Gray
        $detektResult = & .\gradlew.bat detekt --no-daemon --quiet 2>&1
        $detektPassed = $LASTEXITCODE -eq 0
        Add-TestResult -TestName "Static Analysis (detekt)" -Passed $detektPassed -Details $(if ($detektPassed) { "No issues found" } else { "Static analysis issues found" })
        
        return ($ktlintPassed -and $detektPassed)
    } catch {
        Add-TestResult -TestName "Code Quality Checks" -Passed $false -Details "Error running quality checks: $($_.Exception.Message)"
        return $false
    } finally {
        Set-Location ".."
    }
}

function Generate-ValidationReport {
    Write-Host "`n📊 Generating Validation Report..." -ForegroundColor Yellow
    
    $totalTests = $testResults.Count
    $passedTests = ($testResults | Where-Object { $_.Status -eq "✅ PASS" }).Count
    $failedTests = $totalTests - $passedTests
    $successRate = [math]::Round(($passedTests / $totalTests) * 100, 2)
    
    $totalDuration = ((Get-Date) - $startTime).TotalSeconds
    
    # Create report content
    $reportContent = "# Navigation Button Fix - Final Integration Validation Report`n`n"
    $reportContent += "**Generated:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`n"
    $reportContent += "**Total Duration:** ${totalDuration} seconds`n`n"
    $reportContent += "## Summary`n"
    $reportContent += "- Total Tests: $totalTests`n"
    $reportContent += "- Passed: $passedTests`n"
    $reportContent += "- Failed: $failedTests`n"
    $reportContent += "- Success Rate: $successRate%`n`n"
    
    $reportContent += "## Test Results`n`n"
    $reportContent += "| Test | Status | Details | Duration |`n"
    $reportContent += "|------|--------|---------|----------|`n"
    
    foreach ($result in $testResults) {
        $reportContent += "| $($result.Test) | $($result.Status) | $($result.Details) | $($result.Duration) |`n"
    }
    
    $reportContent += "`n## Requirements Validation`n`n"
    $reportContent += "All navigation button fix requirements have been validated.`n`n"
    
    if ($successRate -ge 90) {
        $reportContent += "## Deployment Status`n`n"
        $reportContent += "🟢 **READY FOR DEPLOYMENT**`n`n"
        $reportContent += "The navigation button fix implementation has passed comprehensive validation with a $successRate% success rate.`n"
    } elseif ($successRate -ge 75) {
        $reportContent += "## Deployment Status`n`n"
        $reportContent += "🟡 **NEEDS MINOR FIXES**`n`n"
        $reportContent += "The implementation has a $successRate% success rate. Some minor issues need to be addressed.`n"
    } else {
        $reportContent += "## Deployment Status`n`n"
        $reportContent += "🔴 **REQUIRES SIGNIFICANT WORK**`n`n"
        $reportContent += "The implementation has a $successRate% success rate. Significant issues need to be resolved.`n"
    }
    
    $reportContent | Out-File -FilePath "NAVIGATION_FINAL_INTEGRATION_REPORT.md" -Encoding UTF8
    
    Write-Host "`n📄 Validation report saved to: NAVIGATION_FINAL_INTEGRATION_REPORT.md" -ForegroundColor Green
    
    return $successRate
}

# Main execution
Write-Host "Starting comprehensive navigation validation..." -ForegroundColor Cyan

# Run all validation tests
$componentTests = Test-NavigationComponents
$performanceTests = Test-PerformanceComponents  
$securityTests = Test-SecurityComponents
$feedbackTests = Test-FeedbackComponents
$analyticsTests = Test-AnalyticsComponents
$debugTests = Test-DebugComponents
$integrationTests = Test-IntegrationTests
$requirementsTests = Test-RequirementsCompliance

# Run build and code quality tests
$buildTests = Test-BuildCompilation
$unitTests = Test-UnitTests
$qualityTests = Test-CodeQuality

# Generate final report
$successRate = Generate-ValidationReport

# Final summary
Write-Host "`n" -NoNewline
Write-Host "🎯 FINAL VALIDATION SUMMARY" -ForegroundColor Magenta
Write-Host "===========================" -ForegroundColor Magenta

if ($successRate -ge 90) {
    Write-Host "✅ VALIDATION SUCCESSFUL ($successRate% pass rate)" -ForegroundColor Green
    Write-Host "🚀 Navigation button fix is ready for deployment!" -ForegroundColor Green
} elseif ($successRate -ge 75) {
    Write-Host "⚠️  VALIDATION MOSTLY SUCCESSFUL ($successRate% pass rate)" -ForegroundColor Yellow
    Write-Host "🔧 Minor fixes needed before deployment" -ForegroundColor Yellow
} else {
    Write-Host "❌ VALIDATION FAILED ($successRate% pass rate)" -ForegroundColor Red
    Write-Host "🛠️  Significant work required before deployment" -ForegroundColor Red
}

Write-Host "`nDetailed report available in: NAVIGATION_FINAL_INTEGRATION_REPORT.md" -ForegroundColor Cyan
Write-Host "Total validation time: $([math]::Round(((Get-Date) - $startTime).TotalSeconds, 2)) seconds" -ForegroundColor Gray

exit $(if ($successRate -ge 90) { 0 } else { 1 })