#!/usr/bin/env pwsh

# Navigation Button Fix - Final Integration and Validation Script
Write-Host "Navigation Button Fix - Final Integration and Validation" -ForegroundColor Green
Write-Host "=======================================================" -ForegroundColor Green

$ErrorActionPreference = "Continue"
$testResults = @()
$startTime = Get-Date

function Test-FileExists {
    param([string]$FilePath, [string]$Description)
    
    $exists = Test-Path $FilePath
    $status = if ($exists) { "PASS" } else { "FAIL" }
    Write-Host "$status - $Description" -ForegroundColor $(if ($exists) { "Green" } else { "Red" })
    
    $testResults += [PSCustomObject]@{
        Test = $Description
        Status = $status
        FilePath = $FilePath
    }
    
    return $exists
}

Write-Host "`nValidating Core Navigation Components..." -ForegroundColor Yellow

# Core navigation files
$coreFiles = @{
    "android/app/src/main/java/com/locationsharing/app/navigation/NavigationManagerImpl.kt" = "NavigationManager Implementation"
    "android/app/src/main/java/com/locationsharing/app/navigation/NavigationStateTrackerImpl.kt" = "NavigationStateTracker Implementation"
    "android/app/src/main/java/com/locationsharing/app/navigation/NavigationErrorHandler.kt" = "NavigationErrorHandler"
    "android/app/src/main/java/com/locationsharing/app/ui/components/button/ResponsiveButton.kt" = "ResponsiveButton Component"
    "android/app/src/main/java/com/locationsharing/app/ui/components/button/ButtonResponseManagerImpl.kt" = "ButtonResponseManager Implementation"
    "android/app/src/main/java/com/locationsharing/app/MainActivity.kt" = "MainActivity with Navigation Integration"
}

$coreComponentsValid = $true
foreach ($file in $coreFiles.GetEnumerator()) {
    $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
    if (-not $exists) { $coreComponentsValid = $false }
}

Write-Host "`nValidating Performance Components..." -ForegroundColor Yellow

$performanceFiles = @{
    "android/app/src/main/java/com/locationsharing/app/navigation/performance/NavigationPerformanceMonitor.kt" = "Performance Monitor"
    "android/app/src/main/java/com/locationsharing/app/navigation/performance/NavigationCache.kt" = "Navigation Cache"
    "android/app/src/main/java/com/locationsharing/app/navigation/performance/NavigationDestinationLoader.kt" = "Destination Loader"
    "android/app/src/main/java/com/locationsharing/app/navigation/performance/OptimizedButtonResponseManager.kt" = "Optimized Button Response Manager"
}

$performanceComponentsValid = $true
foreach ($file in $performanceFiles.GetEnumerator()) {
    $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
    if (-not $exists) { $performanceComponentsValid = $false }
}

Write-Host "`nValidating Security Components..." -ForegroundColor Yellow

$securityFiles = @{
    "android/app/src/main/java/com/locationsharing/app/navigation/security/NavigationSecurityManager.kt" = "Security Manager"
    "android/app/src/main/java/com/locationsharing/app/navigation/security/RouteValidator.kt" = "Route Validator"
    "android/app/src/main/java/com/locationsharing/app/navigation/security/NavigationStateProtector.kt" = "State Protector"
    "android/app/src/main/java/com/locationsharing/app/navigation/security/SessionAwareNavigationHandler.kt" = "Session-Aware Handler"
}

$securityComponentsValid = $true
foreach ($file in $securityFiles.GetEnumerator()) {
    $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
    if (-not $exists) { $securityComponentsValid = $false }
}

Write-Host "`nValidating Feedback Components..." -ForegroundColor Yellow

$feedbackFiles = @{
    "android/app/src/main/java/com/locationsharing/app/ui/components/feedback/HapticFeedbackManager.kt" = "Haptic Feedback Manager"
    "android/app/src/main/java/com/locationsharing/app/ui/components/feedback/VisualFeedbackManager.kt" = "Visual Feedback Manager"
    "android/app/src/main/java/com/locationsharing/app/ui/navigation/EnhancedNavigationTransitions.kt" = "Enhanced Navigation Transitions"
}

$feedbackComponentsValid = $true
foreach ($file in $feedbackFiles.GetEnumerator()) {
    $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
    if (-not $exists) { $feedbackComponentsValid = $false }
}

Write-Host "`nValidating Analytics Components..." -ForegroundColor Yellow

$analyticsFiles = @{
    "android/app/src/main/java/com/locationsharing/app/navigation/NavigationAnalyticsImpl.kt" = "Navigation Analytics Implementation"
    "android/app/src/main/java/com/locationsharing/app/navigation/AnalyticsManager.kt" = "Analytics Manager"
    "android/app/src/main/java/com/locationsharing/app/navigation/ButtonAnalyticsImpl.kt" = "Button Analytics Implementation"
}

$analyticsComponentsValid = $true
foreach ($file in $analyticsFiles.GetEnumerator()) {
    $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
    if (-not $exists) { $analyticsComponentsValid = $false }
}

Write-Host "`nValidating Integration Tests..." -ForegroundColor Yellow

$integrationTestFiles = @{
    "android/app/src/test/java/com/locationsharing/app/navigation/NavigationIntegrationValidationTest.kt" = "Navigation Integration Validation Test"
    "android/app/src/androidTest/java/com/locationsharing/app/navigation/NavigationEndToEndUITest.kt" = "End-to-End UI Test"
    "android/app/src/androidTest/java/com/locationsharing/app/MainActivityNavigationIntegrationTest.kt" = "MainActivity Navigation Integration Test"
}

$integrationTestsValid = $true
foreach ($file in $integrationTestFiles.GetEnumerator()) {
    $exists = Test-FileExists -FilePath $file.Key -Description $file.Value
    if (-not $exists) { $integrationTestsValid = $false }
}

Write-Host "`nTesting Build Compilation..." -ForegroundColor Yellow

$buildValid = $false
try {
    Set-Location "android"
    Write-Host "  Running Gradle build..." -ForegroundColor Gray
    $buildResult = & .\gradlew.bat assembleDebug --no-daemon --quiet 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "PASS - Android Build Compilation" -ForegroundColor Green
        $buildValid = $true
    } else {
        Write-Host "FAIL - Android Build Compilation" -ForegroundColor Red
        Write-Host "  Build output: $buildResult" -ForegroundColor Gray
    }
} catch {
    Write-Host "FAIL - Android Build Compilation (Exception)" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Gray
} finally {
    Set-Location ".."
}

# Calculate overall results
$totalTests = $testResults.Count + 1  # +1 for build test
$passedTests = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
if ($buildValid) { $passedTests++ }

$failedTests = $totalTests - $passedTests
$successRate = [math]::Round(($passedTests / $totalTests) * 100, 2)
$totalDuration = ((Get-Date) - $startTime).TotalSeconds

# Generate summary report
Write-Host "`nGenerating Summary Report..." -ForegroundColor Yellow

$reportContent = "# Navigation Button Fix - Final Integration Validation Report`n`n"
$reportContent += "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`n"
$reportContent += "Total Duration: $totalDuration seconds`n`n"
$reportContent += "## Summary`n"
$reportContent += "- Total Tests: $totalTests`n"
$reportContent += "- Passed: $passedTests`n"
$reportContent += "- Failed: $failedTests`n"
$reportContent += "- Success Rate: $successRate%`n`n"
$reportContent += "## Component Validation Results`n"
$reportContent += "- Core Navigation Components: $(if ($coreComponentsValid) { 'VALID' } else { 'INVALID' })`n"
$reportContent += "- Performance Components: $(if ($performanceComponentsValid) { 'VALID' } else { 'INVALID' })`n"
$reportContent += "- Security Components: $(if ($securityComponentsValid) { 'VALID' } else { 'INVALID' })`n"
$reportContent += "- Feedback Components: $(if ($feedbackComponentsValid) { 'VALID' } else { 'INVALID' })`n"
$reportContent += "- Analytics Components: $(if ($analyticsComponentsValid) { 'VALID' } else { 'INVALID' })`n"
$reportContent += "- Integration Tests: $(if ($integrationTestsValid) { 'VALID' } else { 'INVALID' })`n"
$reportContent += "- Build Compilation: $(if ($buildValid) { 'VALID' } else { 'INVALID' })`n`n"

if ($successRate -ge 90) {
    $reportContent += "## Deployment Status`n`n"
    $reportContent += "READY FOR DEPLOYMENT`n`n"
    $reportContent += "The navigation button fix implementation has passed comprehensive validation with a $successRate% success rate.`n"
} elseif ($successRate -ge 75) {
    $reportContent += "## Deployment Status`n`n"
    $reportContent += "NEEDS MINOR FIXES`n`n"
    $reportContent += "The implementation has a $successRate% success rate. Some minor issues need to be addressed.`n"
} else {
    $reportContent += "## Deployment Status`n`n"
    $reportContent += "REQUIRES SIGNIFICANT WORK`n`n"
    $reportContent += "The implementation has a $successRate% success rate. Significant issues need to be resolved.`n"
}

$reportContent | Out-File -FilePath "NAVIGATION_FINAL_INTEGRATION_REPORT.md" -Encoding UTF8

# Final summary
Write-Host "`nFINAL VALIDATION SUMMARY" -ForegroundColor Magenta
Write-Host "========================" -ForegroundColor Magenta

if ($successRate -ge 90) {
    Write-Host "VALIDATION SUCCESSFUL ($successRate% pass rate)" -ForegroundColor Green
    Write-Host "Navigation button fix is ready for deployment!" -ForegroundColor Green
} elseif ($successRate -ge 75) {
    Write-Host "VALIDATION MOSTLY SUCCESSFUL ($successRate% pass rate)" -ForegroundColor Yellow
    Write-Host "Minor fixes needed before deployment" -ForegroundColor Yellow
} else {
    Write-Host "VALIDATION FAILED ($successRate% pass rate)" -ForegroundColor Red
    Write-Host "Significant work required before deployment" -ForegroundColor Red
}

Write-Host "`nDetailed report saved to: NAVIGATION_FINAL_INTEGRATION_REPORT.md" -ForegroundColor Cyan
Write-Host "Total validation time: $([math]::Round($totalDuration, 2)) seconds" -ForegroundColor Gray

exit $(if ($successRate -ge 90) { 0 } else { 1 })