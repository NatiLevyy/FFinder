#!/usr/bin/env pwsh

# FFinder Hero Section Implementation Validation Script
# Validates that Task 3: Animated Hero Section Implementation meets all requirements

Write-Host "=== FFinder Hero Section Implementation Validation ===" -ForegroundColor Cyan
Write-Host ""

$ErrorCount = 0
$SuccessCount = 0

function Test-Requirement {
    param(
        [string]$Description,
        [scriptblock]$Test
    )
    
    Write-Host "Testing: $Description" -ForegroundColor Yellow
    
    try {
        $result = & $Test
        if ($result) {
            Write-Host "✅ PASS: $Description" -ForegroundColor Green
            $script:SuccessCount++
        } else {
            Write-Host "❌ FAIL: $Description" -ForegroundColor Red
            $script:ErrorCount++
        }
    } catch {
        Write-Host "❌ ERROR: $Description - $($_.Exception.Message)" -ForegroundColor Red
        $script:ErrorCount++
    }
    
    Write-Host ""
}

# Test 1: HeroSection component exists
Test-Requirement "HeroSection component file exists" {
    Test-Path "android/app/src/main/java/com/locationsharing/app/ui/home/components/HeroSection.kt"
}

# Test 2: HeroSection contains logo_full asset reference
Test-Requirement "HeroSection uses logo_full asset at 96dp height" {
    $content = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/home/components/HeroSection.kt" -Raw
    ($content -match "logo_full_vector") -and ($content -match "\.height\(96\.dp\)")
}

# Test 3: Logo fade-in animation with 1000ms duration and EaseOut easing
Test-Requirement "Logo fade-in animation with 1000ms duration using EaseOut easing" {
    $content = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/home/components/HeroSection.kt" -Raw
    ($content -match "animateFloatAsState") -and 
    ($content -match "tween\(1000.*EaseOut\)") -and
    ($content -match "logoAlpha")
}

# Test 4: Slow zoom animation (1.0 → 1.1 → 1.0 scale over 4 seconds) with infiniteRepeatable
Test-Requirement "Slow zoom animation (1.0 → 1.1 → 1.0 scale over 4 seconds) with infiniteRepeatable" {
    $content = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/home/components/HeroSection.kt" -Raw
    ($content -match "1\.1f") -and 
    ($content -match "infiniteRepeatable") -and
    ($content -match "tween\(4000.*EaseInOutCubic\)") -and
    ($content -match "RepeatMode\.Reverse") -and
    ($content -match "logoScale")
}

# Test 5: Subtitle text with bodyMedium typography, white color, center alignment
Test-Requirement "Subtitle text with bodyMedium typography, white color, center alignment" {
    $content = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/home/components/HeroSection.kt" -Raw
    ($content -match "Share your live location and find friends instantly") -and
    ($content -match "MaterialTheme\.typography\.bodyMedium") -and
    ($content -match "Color\.White") -and
    ($content -match "TextAlign\.Center")
}

# Test 6: Accessibility support to respect animation preferences
Test-Requirement "Accessibility support to respect animation preferences" {
    $content = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/home/components/HeroSection.kt" -Raw
    ($content -match "animationsEnabled.*Boolean") -and
    ($content -match "if \(animationsEnabled\)") -and
    ($content -match "contentDescription.*FFinder")
}

# Test 7: Component has proper imports and structure
Test-Requirement "Component has proper imports and Compose structure" {
    $content = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/home/components/HeroSection.kt" -Raw
    ($content -match "@Composable") -and
    ($content -match "fun HeroSection") -and
    ($content -match "import androidx\.compose") -and
    ($content -match "import com\.locationsharing\.app\.R")
}

# Test 8: Unit tests exist
Test-Requirement "Unit tests for HeroSection exist" {
    (Test-Path "android/app/src/test/java/com/locationsharing/app/ui/home/components/HeroSectionTest.kt") -and
    (Test-Path "android/app/src/test/java/com/locationsharing/app/ui/home/components/HeroSectionAccessibilityTest.kt") -and
    (Test-Path "android/app/src/test/java/com/locationsharing/app/ui/home/components/HeroSectionAnimationTest.kt")
}

# Test 9: Integration tests exist
Test-Requirement "Integration tests for HeroSection exist" {
    Test-Path "android/app/src/test/java/com/locationsharing/app/ui/home/components/HeroSectionIntegrationTest.kt"
}

# Test 10: Preview composables exist
Test-Requirement "Preview composables for HeroSection exist" {
    $content = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/home/components/HeroSection.kt" -Raw
    ($content -match "@Preview") -and
    ($content -match "HeroSectionPreview") -and
    ($content -match "HeroSectionAnimationsDisabledPreview") -and
    ($content -match "HeroSectionDarkPreview")
}

# Test 11: Project compiles successfully
Test-Requirement "Project compiles successfully" {
    Push-Location "android"
    try {
        $result = & ./gradlew compileDebugKotlin --quiet
        $LASTEXITCODE -eq 0
    } finally {
        Pop-Location
    }
}

# Summary
Write-Host "=== Validation Summary ===" -ForegroundColor Cyan
Write-Host "✅ Passed: $SuccessCount tests" -ForegroundColor Green
Write-Host "❌ Failed: $ErrorCount tests" -ForegroundColor Red
Write-Host ""

if ($ErrorCount -eq 0) {
    Write-Host "🎉 All requirements met! Hero Section implementation is complete." -ForegroundColor Green
    Write-Host ""
    Write-Host "Task 3: Animated Hero Section Implementation - COMPLETED ✅" -ForegroundColor Green
    Write-Host ""
    Write-Host "Key Features Implemented:" -ForegroundColor Cyan
    Write-Host "• HeroSection composable with logo_full asset at 96dp height" -ForegroundColor White
    Write-Host "• Logo fade-in animation with 1000ms duration using EaseOut easing" -ForegroundColor White
    Write-Host "• Slow zoom animation (1.0 → 1.1 → 1.0 scale over 4 seconds) with infiniteRepeatable" -ForegroundColor White
    Write-Host "• Subtitle text with bodyMedium typography, white color, center alignment" -ForegroundColor White
    Write-Host "• Accessibility support to respect animation preferences" -ForegroundColor White
    Write-Host "• Comprehensive unit tests and integration tests" -ForegroundColor White
    Write-Host "• Preview composables for design validation" -ForegroundColor White
    exit 0
} else {
    Write-Host "❌ Some requirements not met. Please review the failed tests above." -ForegroundColor Red
    exit 1
}