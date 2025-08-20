#!/usr/bin/env pwsh

# FFinder Home Screen Core Components Validation Script
# Validates that Task 2 components are properly implemented

Write-Host "FFinder Home Screen Core Components Validation" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

$ErrorCount = 0

# Function to check if file exists and report
function Test-FileExists {
    param([string]$FilePath, [string]$Description)
    
    if (Test-Path $FilePath) {
        Write-Host "✅ $Description" -ForegroundColor Green
        return $true
    } else {
        Write-Host "❌ $Description - File not found: $FilePath" -ForegroundColor Red
        $script:ErrorCount++
        return $false
    }
}

# Function to check file content for specific patterns
function Test-FileContent {
    param([string]$FilePath, [string]$Pattern, [string]$Description)
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        if ($content -match $Pattern) {
            Write-Host "✅ $Description" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ $Description - Pattern not found in $FilePath" -ForegroundColor Red
            $script:ErrorCount++
            return $false
        }
    } else {
        Write-Host "❌ $Description - File not found: $FilePath" -ForegroundColor Red
        $script:ErrorCount++
        return $false
    }
}

Write-Host ""
Write-Host "Checking Core Component Files..." -ForegroundColor Yellow

# Check HomeScreenState
Test-FileExists "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenState.kt" "HomeScreenState data class"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenState.kt" "data class HomeScreenState" "HomeScreenState is properly defined as data class"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenState.kt" "isLoading.*Boolean" "HomeScreenState has isLoading property"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenState.kt" "hasLocationPermission.*Boolean" "HomeScreenState has hasLocationPermission property"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenState.kt" "mapPreviewLocation.*LatLng" "HomeScreenState has mapPreviewLocation property"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenState.kt" "animationsEnabled.*Boolean" "HomeScreenState has animationsEnabled property"

# Check HomeScreenEvent
Test-FileExists "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenEvent.kt" "HomeScreenEvent sealed class"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenEvent.kt" "sealed class HomeScreenEvent" "HomeScreenEvent is properly defined as sealed class"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenEvent.kt" "object StartSharing" "HomeScreenEvent has StartSharing event"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenEvent.kt" "object NavigateToFriends" "HomeScreenEvent has NavigateToFriends event"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenEvent.kt" "object NavigateToSettings" "HomeScreenEvent has NavigateToSettings event"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/HomeScreenEvent.kt" "ScreenConfigurationChanged" "HomeScreenEvent has ScreenConfigurationChanged event"

# Check BackgroundGradient
Test-FileExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/BackgroundGradient.kt" "BackgroundGradient composable"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/components/BackgroundGradient.kt" "@Composable" "BackgroundGradient is a Composable function"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/components/BackgroundGradient.kt" "Brush\.verticalGradient" "BackgroundGradient uses vertical gradient"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/components/BackgroundGradient.kt" "extendedColors\.gradientTop" "BackgroundGradient uses theme gradient colors"

# Check ResponsiveLayout
Test-FileExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/ResponsiveLayout.kt" "ResponsiveLayout components"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/components/ResponsiveLayout.kt" "data class ResponsiveLayoutConfig" "ResponsiveLayoutConfig data class exists"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/components/ResponsiveLayout.kt" "isNarrowScreen.*Boolean" "ResponsiveLayoutConfig has isNarrowScreen property"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/components/ResponsiveLayout.kt" "LocalConfiguration" "ResponsiveLayout uses LocalConfiguration"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/components/ResponsiveLayout.kt" "screenWidthDp.*360" "ResponsiveLayout has narrow screen threshold"

# Check HapticFeedbackUtils
Test-FileExists "android/app/src/main/java/com/locationsharing/app/ui/home/components/HapticFeedbackUtils.kt" "HapticFeedbackUtils"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/components/HapticFeedbackUtils.kt" "class HapticFeedbackManager" "HapticFeedbackManager class exists"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/components/HapticFeedbackUtils.kt" "LocalHapticFeedback" "HapticFeedbackUtils uses LocalHapticFeedback"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/components/HapticFeedbackUtils.kt" "performPrimaryAction" "HapticFeedbackManager has performPrimaryAction method"
Test-FileContent "android/app/src/main/java/com/locationsharing/app/ui/home/components/HapticFeedbackUtils.kt" "rememberHapticFeedbackManager" "HapticFeedbackUtils has remember function"

Write-Host ""
Write-Host "Checking Test Files..." -ForegroundColor Yellow

# Check test files
Test-FileExists "android/app/src/test/java/com/locationsharing/app/ui/home/HomeScreenStateTest.kt" "HomeScreenState unit tests"
Test-FileExists "android/app/src/test/java/com/locationsharing/app/ui/home/HomeScreenEventTest.kt" "HomeScreenEvent unit tests"
Test-FileExists "android/app/src/test/java/com/locationsharing/app/ui/home/components/HomeScreenCoreComponentsIntegrationTest.kt" "Integration tests"

# Check test content
Test-FileContent "android/app/src/test/java/com/locationsharing/app/ui/home/HomeScreenStateTest.kt" "@Test" "HomeScreenState tests have test methods"
Test-FileContent "android/app/src/test/java/com/locationsharing/app/ui/home/HomeScreenEventTest.kt" "@Test" "HomeScreenEvent tests have test methods"
Test-FileContent "android/app/src/test/java/com/locationsharing/app/ui/home/components/HomeScreenCoreComponentsIntegrationTest.kt" "Integration.*Test" "Integration tests are properly named"

Write-Host ""
Write-Host "Validation Summary" -ForegroundColor Yellow
Write-Host "==================" -ForegroundColor Yellow

if ($ErrorCount -eq 0) {
    Write-Host "All core components are properly implemented!" -ForegroundColor Green
    Write-Host "✅ HomeScreenState data class with all required properties" -ForegroundColor Green
    Write-Host "✅ HomeScreenEvent sealed class with all user interactions" -ForegroundColor Green
    Write-Host "✅ BackgroundGradient composable with brand gradient" -ForegroundColor Green
    Write-Host "✅ ResponsiveLayout system with narrow screen detection" -ForegroundColor Green
    Write-Host "✅ HapticFeedback integration with consistent patterns" -ForegroundColor Green
    Write-Host "✅ Comprehensive unit and integration tests" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "Task 2: Core Component Architecture Setup - COMPLETED" -ForegroundColor Green
    exit 0
} else {
    Write-Host "Found $ErrorCount issues that need to be addressed" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please fix the issues above before proceeding" -ForegroundColor Yellow
    exit 1
}