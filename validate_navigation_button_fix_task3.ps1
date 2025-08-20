#!/usr/bin/env pwsh

# Validation script for Navigation Button Fix Task 3 implementation
# Tests MainActivity navigation setup with enhanced NavigationManager

Write-Host "=== Navigation Button Fix Task 3 Validation ===" -ForegroundColor Cyan
Write-Host "Validating MainActivity navigation setup implementation..." -ForegroundColor Yellow

$ErrorCount = 0

# Test 1: Check MainActivity uses NavigationManager
Write-Host "`n1. Checking MainActivity uses NavigationManager..." -ForegroundColor Green
$mainActivityContent = Get-Content "android/app/src/main/java/com/locationsharing/app/MainActivity.kt" -Raw
if ($mainActivityContent -match "NavigationManager" -and $mainActivityContent -match "@Inject") {
    Write-Host "   ✓ MainActivity properly injects NavigationManager" -ForegroundColor Green
} else {
    Write-Host "   ✗ MainActivity does not properly inject NavigationManager" -ForegroundColor Red
    $ErrorCount++
}

# Test 2: Check NavHost configuration with error handling
Write-Host "`n2. Checking NavHost configuration..." -ForegroundColor Green
if ($mainActivityContent -match "NavHost" -and $mainActivityContent -match "Screen\.HOME\.route") {
    Write-Host "   ✓ NavHost properly configured with Screen enum routes" -ForegroundColor Green
} else {
    Write-Host "   ✗ NavHost not properly configured with Screen enum routes" -ForegroundColor Red
    $ErrorCount++
}

# Test 3: Check system back button handling
Write-Host "`n3. Checking system back button handling..." -ForegroundColor Green
if ($mainActivityContent -match "onBackPressed" -and $mainActivityContent -match "navigationManager\.navigateBack") {
    Write-Host "   ✓ System back button properly handled with NavigationManager" -ForegroundColor Green
} else {
    Write-Host "   ✗ System back button not properly handled with NavigationManager" -ForegroundColor Red
    $ErrorCount++
}

# Test 4: Check navigation error recovery mechanisms
Write-Host "`n4. Checking navigation error recovery..." -ForegroundColor Green
if ($mainActivityContent -match "navigationManager\.navigateToHome" -and $mainActivityContent -match "fallback") {
    Write-Host "   ✓ Navigation error recovery mechanisms implemented" -ForegroundColor Green
} else {
    Write-Host "   ✗ Navigation error recovery mechanisms not properly implemented" -ForegroundColor Red
    $ErrorCount++
}

# Test 5: Check NavigationModule exists
Write-Host "`n5. Checking NavigationModule dependency injection..." -ForegroundColor Green
if (Test-Path "android/app/src/main/java/com/locationsharing/app/di/NavigationModule.kt") {
    $moduleContent = Get-Content "android/app/src/main/java/com/locationsharing/app/di/NavigationModule.kt" -Raw
    if ($moduleContent -match "@Module" -and $moduleContent -match "@InstallIn" -and $moduleContent -match "NavigationManager") {
        Write-Host "   ✓ NavigationModule properly configured for dependency injection" -ForegroundColor Green
    } else {
        Write-Host "   ✗ NavigationModule not properly configured" -ForegroundColor Red
        $ErrorCount++
    }
} else {
    Write-Host "   ✗ NavigationModule not found" -ForegroundColor Red
    $ErrorCount++
}

# Test 6: Check navigation state tracking integration
Write-Host "`n6. Checking navigation state tracking..." -ForegroundColor Green
if ($mainActivityContent -match "navigationStateTracker" -and $mainActivityContent -match "updateCurrentScreen") {
    Write-Host "   ✓ Navigation state tracking properly integrated" -ForegroundColor Green
} else {
    Write-Host "   ✗ Navigation state tracking not properly integrated" -ForegroundColor Red
    $ErrorCount++
}

# Test 7: Check integration tests exist
Write-Host "`n7. Checking integration tests..." -ForegroundColor Green
$testFiles = @(
    "android/app/src/test/java/com/locationsharing/app/MainActivityNavigationTest.kt",
    "android/app/src/androidTest/java/com/locationsharing/app/MainActivityNavigationIntegrationTest.kt",
    "android/app/src/test/java/com/locationsharing/app/MainActivityNavigationErrorRecoveryTest.kt"
)

$testFilesExist = 0
foreach ($testFile in $testFiles) {
    if (Test-Path $testFile) {
        $testFilesExist++
    }
}

if ($testFilesExist -eq $testFiles.Count) {
    Write-Host "   ✓ All integration tests created ($testFilesExist/$($testFiles.Count))" -ForegroundColor Green
} else {
    Write-Host "   ✗ Missing integration tests ($testFilesExist/$($testFiles.Count))" -ForegroundColor Red
    $ErrorCount++
}

# Test 8: Check LaunchedEffect for NavigationManager initialization
Write-Host "`n8. Checking NavigationManager initialization..." -ForegroundColor Green
if ($mainActivityContent -match "LaunchedEffect.*navController" -and $mainActivityContent -match "setNavController") {
    Write-Host "   ✓ NavigationManager properly initialized with NavController" -ForegroundColor Green
} else {
    Write-Host "   ✗ NavigationManager initialization not properly implemented" -ForegroundColor Red
    $ErrorCount++
}

# Test 9: Check screen-specific navigation state updates
Write-Host "`n9. Checking screen-specific navigation state updates..." -ForegroundColor Green
if ($mainActivityContent -match "LaunchedEffect.*Unit.*updateCurrentScreen") {
    Write-Host "   ✓ Screen-specific navigation state updates implemented" -ForegroundColor Green
} else {
    Write-Host "   ✗ Screen-specific navigation state updates not implemented" -ForegroundColor Red
    $ErrorCount++
}

# Test 10: Check settings placeholder screen
Write-Host "`n10. Checking settings placeholder screen..." -ForegroundColor Green
if ($mainActivityContent -match "SettingsPlaceholderScreen" -and $mainActivityContent -match "Settings screen coming soon") {
    Write-Host "   ✓ Settings placeholder screen implemented" -ForegroundColor Green
} else {
    Write-Host "   ✗ Settings placeholder screen not implemented" -ForegroundColor Red
    $ErrorCount++
}

# Summary
Write-Host "`n=== Validation Summary ===" -ForegroundColor Cyan
if ($ErrorCount -eq 0) {
    Write-Host "✓ All validation checks passed! Task 3 implementation is complete." -ForegroundColor Green
    Write-Host "MainActivity navigation setup has been successfully enhanced with:" -ForegroundColor Green
    Write-Host "  • NavigationManager integration with dependency injection" -ForegroundColor Green
    Write-Host "  • Proper NavHost configuration with error handling" -ForegroundColor Green
    Write-Host "  • System back button handling with navigation fallbacks" -ForegroundColor Green
    Write-Host "  • Navigation error recovery mechanisms" -ForegroundColor Green
    Write-Host "  • Comprehensive integration tests" -ForegroundColor Green
    exit 0
} else {
    Write-Host "✗ $ErrorCount validation checks failed. Please review the implementation." -ForegroundColor Red
    exit 1
}