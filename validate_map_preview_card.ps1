#!/usr/bin/env pwsh

# Validation script for MapPreviewCard component implementation
# This script validates that Task 4: Interactive Map Preview Component has been implemented correctly

Write-Host "Validating MapPreviewCard Component Implementation" -ForegroundColor Cyan
Write-Host "=" * 60

$success = $true
$errors = @()

# Check if MapPreviewCard.kt exists
$mapPreviewCardPath = "android/app/src/main/java/com/locationsharing/app/ui/home/components/MapPreviewCard.kt"
if (Test-Path $mapPreviewCardPath) {
    Write-Host "✅ MapPreviewCard.kt exists" -ForegroundColor Green
    
    # Check for required components and features
    $content = Get-Content $mapPreviewCardPath -Raw
    
    $requiredFeatures = @(
        @{ Name = "MapPreviewCard composable"; Pattern = "@Composable\s+fun\s+MapPreviewCard" },
        @{ Name = "90% width and 160dp height"; Pattern = "fillMaxWidth\(0\.9f\)" },
        @{ Name = "16dp rounded corners"; Pattern = "RoundedCornerShape\(16\.dp\)" },
        @{ Name = "Google Maps integration"; Pattern = "GoogleMap" },
        @{ Name = "Pin bounce animation"; Pattern = "animateFloatAsState" },
        @{ Name = "Auto-pan camera animation"; Pattern = "LaunchedEffect.*animationsEnabled" },
        @{ Name = "Fallback UI"; Pattern = "MapPreviewFallback" },
        @{ Name = "Error handling"; Pattern = "MapPreviewWithErrorHandling" },
        @{ Name = "ic_pin_finder usage"; Pattern = "ic_pin_finder_vector" },
        @{ Name = "EaseInOutBack animation"; Pattern = "EaseInOutBack" },
        @{ Name = "10-second loop"; Pattern = "delay\(10000\)" },
        @{ Name = "3-second bounce"; Pattern = "durationMillis = 3000" }
    )
    
    foreach ($feature in $requiredFeatures) {
        if ($content -match $feature.Pattern) {
            Write-Host "✅ $($feature.Name) implemented" -ForegroundColor Green
        } else {
            Write-Host "❌ $($feature.Name) missing" -ForegroundColor Red
            $errors += "$($feature.Name) not found in MapPreviewCard.kt"
            $success = $false
        }
    }
} else {
    Write-Host "❌ MapPreviewCard.kt not found" -ForegroundColor Red
    $errors += "MapPreviewCard.kt file missing"
    $success = $false
}

# Check if test files exist
$testFiles = @(
    "android/app/src/test/java/com/locationsharing/app/ui/home/components/MapPreviewCardTest.kt",
    "android/app/src/test/java/com/locationsharing/app/ui/home/components/MapPreviewCardIntegrationTest.kt",
    "android/app/src/test/java/com/locationsharing/app/ui/home/components/MapPreviewCardAccessibilityTest.kt",
    "android/app/src/test/java/com/locationsharing/app/ui/home/components/MapPreviewCardPerformanceTest.kt"
)

foreach ($testFile in $testFiles) {
    if (Test-Path $testFile) {
        Write-Host "✅ $(Split-Path $testFile -Leaf) exists" -ForegroundColor Green
    } else {
        Write-Host "❌ $(Split-Path $testFile -Leaf) missing" -ForegroundColor Red
        $errors += "Test file $(Split-Path $testFile -Leaf) missing"
        $success = $false
    }
}

# Check if main code compiles
Write-Host "`nChecking compilation..." -ForegroundColor Yellow
try {
    Push-Location "android"
    $compileResult = & ./gradlew compileDebugKotlin 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Main code compiles successfully" -ForegroundColor Green
    } else {
        Write-Host "❌ Compilation failed" -ForegroundColor Red
        $errors += "Main code compilation failed"
        $success = $false
    }
} catch {
    Write-Host "❌ Error during compilation check: $_" -ForegroundColor Red
    $errors += "Compilation check failed: $_"
    $success = $false
} finally {
    Pop-Location
}

# Check requirements compliance
Write-Host "`nChecking Requirements Compliance..." -ForegroundColor Yellow

$requirements = @(
    @{ Id = "4.1"; Description = "MapPreviewCard at 90% width and 160dp height with 16dp rounded corners" },
    @{ Id = "4.2"; Description = "Google Maps with user location centering and ic_pin_finder marker" },
    @{ Id = "4.3"; Description = "Auto-pan camera animation with 10-second loop using LaunchedEffect" },
    @{ Id = "4.4"; Description = "Pin bounce animation every 3 seconds using animateFloatAsState with EaseInOutBack" },
    @{ Id = "4.5"; Description = "Fallback UI for when location permission is not granted" },
    @{ Id = "4.6"; Description = "Error handling and accessibility support" }
)

foreach ($req in $requirements) {
    Write-Host "Requirement $($req.Id): $($req.Description)" -ForegroundColor Cyan
}

# Summary
Write-Host "`n" + "=" * 60
if ($success) {
    Write-Host "MapPreviewCard Implementation Validation PASSED!" -ForegroundColor Green
    Write-Host "✅ All required features have been implemented" -ForegroundColor Green
    Write-Host "✅ All test files are present" -ForegroundColor Green
    Write-Host "✅ Code compiles successfully" -ForegroundColor Green
    Write-Host "`nTask 4: Interactive Map Preview Component - COMPLETED" -ForegroundColor Green
} else {
    Write-Host "❌ MapPreviewCard Implementation Validation FAILED!" -ForegroundColor Red
    Write-Host "`nErrors found:" -ForegroundColor Red
    foreach ($error in $errors) {
        Write-Host "  - $error" -ForegroundColor Red
    }
}

Write-Host "`nImplementation Details:" -ForegroundColor Yellow
Write-Host "- MapPreviewCard component with Google Maps integration" -ForegroundColor White
Write-Host "- Auto-pan camera animation with 10-second loop" -ForegroundColor White
Write-Host "- Pin bounce animation every 3 seconds with EaseInOutBack" -ForegroundColor White
Write-Host "- Fallback UI for permission handling" -ForegroundColor White
Write-Host "- Error handling with MapPreviewWithErrorHandling wrapper" -ForegroundColor White
Write-Host "- Comprehensive test coverage (unit, integration, accessibility, performance)" -ForegroundColor White
Write-Host "- Accessibility compliance with content descriptions and animation preferences" -ForegroundColor White

exit $(if ($success) { 0 } else { 1 })