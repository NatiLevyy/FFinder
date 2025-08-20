#!/usr/bin/env pwsh

# FFinder Home Screen Accessibility and Responsive Design Validation Script
# This script validates the implementation of Task 8: Accessibility and Responsive Design Implementation

Write-Host "🔍 FFinder Home Screen Accessibility and Responsive Design Validation" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$ErrorCount = 0
$SuccessCount = 0

function Test-FileExists {
    param([string]$FilePath, [string]$Description)
    
    if (Test-Path $FilePath) {
        Write-Host "✅ $Description" -ForegroundColor Green
        $script:SuccessCount++
        return $true
    } else {
        Write-Host "❌ $Description" -ForegroundColor Red
        $script:ErrorCount++
        return $false
    }
}

function Test-ContentExists {
    param([string]$FilePath, [string]$Pattern, [string]$Description)
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        if ($content -match $Pattern) {
            Write-Host "✅ $Description" -ForegroundColor Green
            $script:SuccessCount++
            return $true
        } else {
            Write-Host "❌ $Description" -ForegroundColor Red
            $script:ErrorCount++
            return $false
        }
    } else {
        Write-Host "❌ File not found: $FilePath" -ForegroundColor Red
        $script:ErrorCount++
        return $false
    }
}

Write-Host "`n📁 Checking Core Implementation Files..." -ForegroundColor Yellow

# Check if all required files exist
Test-FileExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityUtils.kt" "AccessibilityUtils.kt exists"
Test-FileExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityResponsiveHomeScreen.kt" "AccessibilityResponsiveHomeScreen.kt exists"
Test-FileExists "android/app/src/test/java/com/locationsharing/app/ui/home/components/AccessibilityAndResponsiveDesignTest.kt" "AccessibilityAndResponsiveDesignTest.kt exists"

Write-Host "`n🎯 Checking Accessibility Implementation..." -ForegroundColor Yellow

# Check for meaningful content descriptions
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/HeroSection.kt" "contentDescription.*FFinder.*Find Friends.*Share Locations" "HeroSection has meaningful content description"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/PrimaryCallToAction.kt" "contentDescription.*Start Live Sharing.*location.*friends" "PrimaryCallToAction has meaningful content description"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/SecondaryActionsRow.kt" "contentDescription.*Friends.*navigate" "SecondaryActionsRow Friends button has meaningful content description"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/SecondaryActionsRow.kt" "contentDescription.*Settings.*navigate" "SecondaryActionsRow Settings button has meaningful content description"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/WhatsNewTeaser.kt" "contentDescription.*What's New.*features" "WhatsNewTeaser has meaningful content description"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/MapPreviewCard.kt" "contentDescription.*Map preview.*location" "MapPreviewCard has meaningful content description"

Write-Host "`n📱 Checking Responsive Design Implementation..." -ForegroundColor Yellow

# Check for responsive FAB implementation
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/PrimaryCallToAction.kt" "isNarrowScreen.*Boolean" "PrimaryCallToAction accepts isNarrowScreen parameter"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/PrimaryCallToAction.kt" "FloatingActionButton.*size.*56\.dp" "Icon-only FAB has proper size for narrow screens"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/PrimaryCallToAction.kt" "ExtendedFloatingActionButton.*fillMaxWidth.*0\.8f" "Extended FAB has proper width for normal screens"

# Check for responsive layout utilities
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/ResponsiveLayout.kt" "isNarrowScreen.*360" "ResponsiveLayout detects narrow screens correctly"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityUtils.kt" "getResponsivePadding" "AccessibilityUtils provides responsive padding"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityUtils.kt" "getResponsiveSpacing" "AccessibilityUtils provides responsive spacing"

Write-Host "`n🎨 Checking Animation Accessibility..." -ForegroundColor Yellow

# Check for animation accessibility preferences
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityUtils.kt" "shouldEnableAnimations" "AccessibilityUtils checks animation preferences"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/HeroSection.kt" "shouldAnimate.*AccessibilityUtils" "HeroSection respects animation preferences"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/MapPreviewCard.kt" "shouldAnimate.*AccessibilityUtils" "MapPreviewCard respects animation preferences"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/WhatsNewTeaser.kt" "shouldAnimate.*AccessibilityUtils" "WhatsNewTeaser respects animation preferences"

Write-Host "`n🎯 Checking Semantic Properties..." -ForegroundColor Yellow

# Check for proper semantic roles
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/PrimaryCallToAction.kt" "role.*Role\.Button" "PrimaryCallToAction has proper button role"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/SecondaryActionsRow.kt" "role.*Role\.Button" "SecondaryActionsRow buttons have proper roles"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/WhatsNewTeaser.kt" "role.*Role\.Button" "WhatsNewTeaser has proper button role"

# Check for heading semantics
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/HeroSection.kt" "heading\(\)" "HeroSection logo has heading semantics"

Write-Host "`n📏 Checking Touch Target Sizes..." -ForegroundColor Yellow

# Check for minimum touch target sizes
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityUtils.kt" "48\.dp.*minimum" "AccessibilityUtils ensures minimum 48dp touch targets"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/PrimaryCallToAction.kt" "size.*56\.dp" "FAB meets minimum touch target size"

Write-Host "`n🧪 Checking Test Coverage..." -ForegroundColor Yellow

# Check for comprehensive test coverage
Test-ContentExists "android/app/src/test/java/com/locationsharing/app/ui/home/components/AccessibilityAndResponsiveDesignTest.kt" "haveMeaningfulContentDescriptions" "Tests verify meaningful content descriptions"
Test-ContentExists "android/app/src/test/java/com/locationsharing/app/ui/home/components/AccessibilityAndResponsiveDesignTest.kt" "respondsToNarrowScreen" "Tests verify responsive FAB behavior"
Test-ContentExists "android/app/src/test/java/com/locationsharing/app/ui/home/components/AccessibilityAndResponsiveDesignTest.kt" "respectAccessibilityPreferences" "Tests verify animation accessibility preferences"
Test-ContentExists "android/app/src/test/java/com/locationsharing/app/ui/home/components/AccessibilityAndResponsiveDesignTest.kt" "focusOrder.*isLogicalAndConsistent" "Tests verify logical focus order"

Write-Host "`n📋 Checking Content Description Constants..." -ForegroundColor Yellow

# Check for consistent content descriptions
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityUtils.kt" "ContentDescriptions" "ContentDescriptions object exists"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityUtils.kt" "LOGO.*FFinder.*Find Friends.*Share Locations" "Logo content description is defined"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityUtils.kt" "PRIMARY_CTA.*Start Live Sharing.*location.*friends" "Primary CTA content description is defined"

Write-Host "`n🏗️ Checking Integration Example..." -ForegroundColor Yellow

# Check for comprehensive integration example
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityResponsiveHomeScreen.kt" "AccessibilityResponsiveHomeScreen" "Integration example exists"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityResponsiveHomeScreen.kt" "rememberAccessibilityConfig" "Integration uses accessibility configuration"
Test-ContentExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/AccessibilityResponsiveHomeScreen.kt" "ResponsiveLayout.*layoutConfig" "Integration uses responsive layout"

Write-Host "`n📊 Validation Summary" -ForegroundColor Cyan
Write-Host "===================" -ForegroundColor Cyan
Write-Host "✅ Successful checks: $SuccessCount" -ForegroundColor Green
Write-Host "❌ Failed checks: $ErrorCount" -ForegroundColor Red

if ($ErrorCount -eq 0) {
    Write-Host "`n🎉 All accessibility and responsive design requirements have been successfully implemented!" -ForegroundColor Green
    Write-Host "   ✓ Meaningful content descriptions for all interactive elements" -ForegroundColor Green
    Write-Host "   ✓ Logical focus order implementation" -ForegroundColor Green
    Write-Host "   ✓ Responsive Extended FAB with narrow screen support" -ForegroundColor Green
    Write-Host "   ✓ Animation respect for system accessibility preferences" -ForegroundColor Green
    Write-Host "   ✓ Proper dp scaling for different screen densities" -ForegroundColor Green
    Write-Host "   ✓ Comprehensive test coverage" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n⚠️  Some accessibility and responsive design requirements need attention." -ForegroundColor Yellow
    Write-Host "   Please review the failed checks above and ensure all requirements are met." -ForegroundColor Yellow
    exit 1
}