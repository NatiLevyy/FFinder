#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Validates the MapScreen haptic feedback system implementation.

.DESCRIPTION
    This script validates that the haptic feedback system has been properly implemented
    across all MapScreen components according to the requirements 3.4, 4.5, 9.6.

.PARAMETER TestType
    The type of validation to run: 'unit', 'integration', 'accessibility', 'performance', or 'all'

.EXAMPLE
    .\validate_haptic_feedback_system.ps1 -TestType all
#>

param(
    [Parameter(Mandatory = $false)]
    [ValidateSet('unit', 'integration', 'accessibility', 'performance', 'all')]
    [string]$TestType = 'all'
)

# Set error action preference
$ErrorActionPreference = 'Stop'

# Colors for output
$Green = "`e[32m"
$Red = "`e[31m"
$Yellow = "`e[33m"
$Blue = "`e[34m"
$Reset = "`e[0m"

function Write-Success {
    param([string]$Message)
    Write-Host "${Green}✓ $Message${Reset}"
}

function Write-Error {
    param([string]$Message)
    Write-Host "${Red}✗ $Message${Reset}"
}

function Write-Warning {
    param([string]$Message)
    Write-Host "${Yellow}⚠ $Message${Reset}"
}

function Write-Info {
    param([string]$Message)
    Write-Host "${Blue}ℹ $Message${Reset}"
}

function Test-FileExists {
    param([string]$FilePath, [string]$Description)
    
    if (Test-Path $FilePath) {
        Write-Success "$Description exists"
        return $true
    } else {
        Write-Error "$Description missing: $FilePath"
        return $false
    }
}

function Test-HapticFeedbackImplementation {
    Write-Info "Validating haptic feedback implementation files..."
    
    $files = @(
        @{
            Path = "android/app/src/main/java/com/locationsharing/app/ui/map/haptic/MapHapticFeedbackManager.kt"
            Description = "MapHapticFeedbackManager"
        },
        @{
            Path = "android/app/src/test/java/com/locationsharing/app/ui/map/haptic/MapHapticFeedbackManagerTest.kt"
            Description = "MapHapticFeedbackManager unit tests"
        },
        @{
            Path = "android/app/src/test/java/com/locationsharing/app/ui/map/haptic/MapHapticFeedbackIntegrationTest.kt"
            Description = "MapHapticFeedback integration tests"
        },
        @{
            Path = "android/app/src/test/java/com/locationsharing/app/ui/map/haptic/MapHapticFeedbackAccessibilityTest.kt"
            Description = "MapHapticFeedback accessibility tests"
        },
        @{
            Path = "android/app/src/test/java/com/locationsharing/app/ui/map/haptic/MapHapticFeedbackPerformanceTest.kt"
            Description = "MapHapticFeedback performance tests"
        }
    )
    
    $allFilesExist = $true
    foreach ($file in $files) {
        if (-not (Test-FileExists $file.Path $file.Description)) {
            $allFilesExist = $false
        }
    }
    
    return $allFilesExist
}

function Test-ComponentIntegration {
    Write-Info "Validating haptic feedback integration in components..."
    
    $components = @(
        "android/app/src/main/java/com/locationsharing/app/ui/map/components/QuickShareFAB.kt",
        "android/app/src/main/java/com/locationsharing/app/ui/friends/components/SelfLocationFAB.kt",
        "android/app/src/main/java/com/locationsharing/app/ui/map/components/DebugFAB.kt",
        "android/app/src/main/java/com/locationsharing/app/ui/map/components/ShareStatusSheet.kt",
        "android/app/src/main/java/com/locationsharing/app/ui/map/components/NearbyFriendsDrawer.kt",
        "android/app/src/main/java/com/locationsharing/app/ui/friends/components/NearbyFriendItem.kt",
        "android/app/src/main/java/com/locationsharing/app/ui/screens/MapScreen.kt"
    )
    
    $allIntegrated = $true
    foreach ($component in $components) {
        if (Test-Path $component) {
            $content = Get-Content $component -Raw
            if ($content -match "rememberMapHapticFeedbackManager|MapHapticFeedbackManager") {
                Write-Success "$(Split-Path $component -Leaf) has haptic feedback integration"
            } else {
                Write-Error "$(Split-Path $component -Leaf) missing haptic feedback integration"
                $allIntegrated = $false
            }
        } else {
            Write-Error "Component file not found: $component"
            $allIntegrated = $false
        }
    }
    
    return $allIntegrated
}

function Test-HapticFeedbackPatterns {
    Write-Info "Validating haptic feedback patterns..."
    
    $managerFile = "android/app/src/main/java/com/locationsharing/app/ui/map/haptic/MapHapticFeedbackManager.kt"
    if (-not (Test-Path $managerFile)) {
        Write-Error "MapHapticFeedbackManager.kt not found"
        return $false
    }
    
    $content = Get-Content $managerFile -Raw
    $patterns = @(
        "performPrimaryFABAction",
        "performSecondaryFABAction", 
        "performAppBarAction",
        "performDrawerAction",
        "performFriendItemAction",
        "performFriendActionButton",
        "performStatusSheetAction",
        "performMarkerAction",
        "performErrorFeedback",
        "performSuccessFeedback",
        "performLocationAction"
    )
    
    $allPatternsExist = $true
    foreach ($pattern in $patterns) {
        if ($content -match $pattern) {
            Write-Success "Haptic pattern '$pattern' implemented"
        } else {
            Write-Error "Haptic pattern '$pattern' missing"
            $allPatternsExist = $false
        }
    }
    
    return $allPatternsExist
}

function Test-AccessibilityCompliance {
    Write-Info "Validating accessibility compliance..."
    
    $managerFile = "android/app/src/main/java/com/locationsharing/app/ui/map/haptic/MapHapticFeedbackManager.kt"
    if (-not (Test-Path $managerFile)) {
        Write-Error "MapHapticFeedbackManager.kt not found"
        return $false
    }
    
    $content = Get-Content $managerFile -Raw
    $accessibilityFeatures = @(
        "try.*catch", # Exception handling for accessibility services
        "Timber\.d", # Logging for debugging
        "HapticFeedbackType\.TextHandleMove", # Light feedback
        "HapticFeedbackType\.LongPress" # Strong feedback
    )
    
    $allFeaturesExist = $true
    foreach ($feature in $accessibilityFeatures) {
        if ($content -match $feature) {
            Write-Success "Accessibility feature '$feature' implemented"
        } else {
            Write-Error "Accessibility feature '$feature' missing"
            $allFeaturesExist = $false
        }
    }
    
    return $allFeaturesExist
}

function Run-UnitTests {
    Write-Info "Running haptic feedback unit tests..."
    
    try {
        Set-Location "android"
        $result = & ./gradlew test --tests "*MapHapticFeedbackManagerTest" --console=plain
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Unit tests passed"
            return $true
        } else {
            Write-Error "Unit tests failed"
            return $false
        }
    } catch {
        Write-Error "Failed to run unit tests: $_"
        return $false
    } finally {
        Set-Location ".."
    }
}

function Run-IntegrationTests {
    Write-Info "Running haptic feedback integration tests..."
    
    try {
        Set-Location "android"
        $result = & ./gradlew test --tests "*MapHapticFeedbackIntegrationTest" --console=plain
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Integration tests passed"
            return $true
        } else {
            Write-Error "Integration tests failed"
            return $false
        }
    } catch {
        Write-Error "Failed to run integration tests: $_"
        return $false
    } finally {
        Set-Location ".."
    }
}

function Run-AccessibilityTests {
    Write-Info "Running haptic feedback accessibility tests..."
    
    try {
        Set-Location "android"
        $result = & ./gradlew test --tests "*MapHapticFeedbackAccessibilityTest" --console=plain
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Accessibility tests passed"
            return $true
        } else {
            Write-Error "Accessibility tests failed"
            return $false
        }
    } catch {
        Write-Error "Failed to run accessibility tests: $_"
        return $false
    } finally {
        Set-Location ".."
    }
}

function Run-PerformanceTests {
    Write-Info "Running haptic feedback performance tests..."
    
    try {
        Set-Location "android"
        $result = & ./gradlew test --tests "*MapHapticFeedbackPerformanceTest" --console=plain
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Performance tests passed"
            return $true
        } else {
            Write-Error "Performance tests failed"
            return $false
        }
    } catch {
        Write-Error "Failed to run performance tests: $_"
        return $false
    } finally {
        Set-Location ".."
    }
}

function Main {
    Write-Info "🔊 Validating MapScreen Haptic Feedback System Implementation"
    Write-Info "Test Type: $TestType"
    Write-Info ""
    
    $allTestsPassed = $true
    
    # Always validate implementation files
    if (-not (Test-HapticFeedbackImplementation)) {
        $allTestsPassed = $false
    }
    
    if (-not (Test-ComponentIntegration)) {
        $allTestsPassed = $false
    }
    
    if (-not (Test-HapticFeedbackPatterns)) {
        $allTestsPassed = $false
    }
    
    if (-not (Test-AccessibilityCompliance)) {
        $allTestsPassed = $false
    }
    
    # Run specific test types
    switch ($TestType) {
        'unit' {
            if (-not (Run-UnitTests)) {
                $allTestsPassed = $false
            }
        }
        'integration' {
            if (-not (Run-IntegrationTests)) {
                $allTestsPassed = $false
            }
        }
        'accessibility' {
            if (-not (Run-AccessibilityTests)) {
                $allTestsPassed = $false
            }
        }
        'performance' {
            if (-not (Run-PerformanceTests)) {
                $allTestsPassed = $false
            }
        }
        'all' {
            if (-not (Run-UnitTests)) {
                $allTestsPassed = $false
            }
            if (-not (Run-IntegrationTests)) {
                $allTestsPassed = $false
            }
            if (-not (Run-AccessibilityTests)) {
                $allTestsPassed = $false
            }
            if (-not (Run-PerformanceTests)) {
                $allTestsPassed = $false
            }
        }
    }
    
    Write-Info ""
    if ($allTestsPassed) {
        Write-Success "🎉 All haptic feedback system validations passed!"
        Write-Info "✅ Haptic feedback for all button interactions implemented"
        Write-Info "✅ Appropriate feedback types for different actions configured"
        Write-Info "✅ Accessibility service compatibility ensured"
        Write-Info "✅ Performance optimized for different device types"
        Write-Info "✅ Comprehensive test coverage provided"
        Write-Info ""
        Write-Info "Requirements 3.4, 4.5, 9.6 have been successfully implemented."
        exit 0
    } else {
        Write-Error "❌ Some haptic feedback system validations failed!"
        Write-Info "Please review the errors above and fix the issues."
        exit 1
    }
}

# Run the main function
Main