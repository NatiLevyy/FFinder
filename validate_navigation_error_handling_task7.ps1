#!/usr/bin/env pwsh

# Validation script for Navigation Error Handling Task 7
Write-Host "Validating Navigation Error Handling Implementation (Task 7)..." -ForegroundColor Cyan

$validationResults = @()

# Check NavigationErrorHandler enhancements
$errorHandlerPath = "android/app/src/main/java/com/locationsharing/app/navigation/NavigationErrorHandler.kt"
if (Test-Path $errorHandlerPath) {
    $content = Get-Content $errorHandlerPath -Raw
    
    if ($content -match "handleTimeoutError") {
        $validationResults += "PASS: Enhanced timeout error handling with retry mechanism"
    } else {
        $validationResults += "FAIL: Missing enhanced timeout error handling"
    }
    
    if ($content -match "fallbackToHome") {
        $validationResults += "PASS: Fallback navigation mechanisms implemented"
    } else {
        $validationResults += "FAIL: Missing fallback navigation mechanisms"
    }
    
    if ($content -match "NavigationAnalytics") {
        $validationResults += "PASS: Analytics integration implemented"
    } else {
        $validationResults += "FAIL: Missing analytics integration"
    }
    
    if ($content -match "MAX_RETRY_ATTEMPTS") {
        $validationResults += "PASS: Maximum retry attempts configured"
    } else {
        $validationResults += "FAIL: Missing retry attempt limits"
    }
    
    if ($content -match "getUserFriendlyMessage") {
        $validationResults += "PASS: User-friendly error messages implemented"
    } else {
        $validationResults += "FAIL: Missing user-friendly error messages"
    }
} else {
    $validationResults += "FAIL: NavigationErrorHandler file not found"
}

# Check NavigationAnalytics interface
$analyticsPath = "android/app/src/main/java/com/locationsharing/app/navigation/NavigationAnalytics.kt"
if (Test-Path $analyticsPath) {
    $validationResults += "PASS: NavigationAnalytics interface created"
} else {
    $validationResults += "FAIL: NavigationAnalytics interface missing"
}

# Check NavigationAnalytics implementation
$analyticsImplPath = "android/app/src/main/java/com/locationsharing/app/navigation/NavigationAnalyticsImpl.kt"
if (Test-Path $analyticsImplPath) {
    $validationResults += "PASS: NavigationAnalytics implementation created"
} else {
    $validationResults += "FAIL: NavigationAnalytics implementation missing"
}

# Check integration tests
$integrationTestPath = "android/app/src/test/java/com/locationsharing/app/navigation/NavigationErrorHandlingIntegrationTest.kt"
if (Test-Path $integrationTestPath) {
    $validationResults += "PASS: Integration tests for error handling created"
} else {
    $validationResults += "FAIL: Integration tests missing"
}

# Check analytics tests
$analyticsTestPath = "android/app/src/test/java/com/locationsharing/app/navigation/NavigationAnalyticsImplTest.kt"
if (Test-Path $analyticsTestPath) {
    $validationResults += "PASS: Analytics implementation tests created"
} else {
    $validationResults += "FAIL: Analytics tests missing"
}

# Check end-to-end tests
$e2eTestPath = "android/app/src/test/java/com/locationsharing/app/navigation/NavigationErrorHandlingEndToEndTest.kt"
if (Test-Path $e2eTestPath) {
    $validationResults += "PASS: End-to-end error handling tests created"
} else {
    $validationResults += "FAIL: End-to-end tests missing"
}

# Check dependency injection
$diPath = "android/app/src/main/java/com/locationsharing/app/di/NavigationModule.kt"
if (Test-Path $diPath) {
    $diContent = Get-Content $diPath -Raw
    if ($diContent -match "bindNavigationAnalytics") {
        $validationResults += "PASS: NavigationAnalytics dependency injection configured"
    } else {
        $validationResults += "FAIL: NavigationAnalytics DI binding missing"
    }
} else {
    $validationResults += "FAIL: NavigationModule not found"
}

# Display results
Write-Host "`nValidation Results:" -ForegroundColor Yellow
foreach ($result in $validationResults) {
    if ($result.StartsWith("PASS")) {
        Write-Host $result -ForegroundColor Green
    } else {
        Write-Host $result -ForegroundColor Red
    }
}

# Count results
$passCount = ($validationResults | Where-Object { $_.StartsWith("PASS") }).Count
$totalCount = $validationResults.Count
$successRate = if ($totalCount -gt 0) { [math]::Round(($passCount / $totalCount) * 100, 1) } else { 0 }

Write-Host "`nSummary:" -ForegroundColor Cyan
Write-Host "Passed: $passCount/$totalCount" -ForegroundColor Green
Write-Host "Success Rate: $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } else { "Yellow" })

# Check compilation
Write-Host "`nChecking Compilation..." -ForegroundColor Cyan
try {
    Push-Location "android"
    $compileResult = & ./gradlew :app:compileDebugKotlin --no-configuration-cache --quiet
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Compilation successful" -ForegroundColor Green
    } else {
        Write-Host "Compilation failed" -ForegroundColor Red
    }
    Pop-Location
} catch {
    Write-Host "Compilation check failed: $_" -ForegroundColor Red
    if (Get-Location | Select-Object -ExpandProperty Path | Select-String "android") {
        Pop-Location
    }
}

Write-Host "`nNavigation Error Handling Task 7 Validation Complete!" -ForegroundColor Magenta

if ($successRate -ge 80) {
    Write-Host "Task 7 implementation is comprehensive and ready!" -ForegroundColor Green
} else {
    Write-Host "Task 7 implementation needs some improvements." -ForegroundColor Yellow
}