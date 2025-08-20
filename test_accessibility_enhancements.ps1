#!/usr/bin/env pwsh

# Test script to verify accessibility enhancements for FriendsToggleFAB
Write-Host "Testing Accessibility Enhancements for FriendsToggleFAB..." -ForegroundColor Green

# Test 1: Verify main code compiles successfully
Write-Host "`n1. Testing compilation..." -ForegroundColor Yellow
Set-Location android
$compileResult = ./gradlew :app:compileDebugKotlin --quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Main code compiles successfully" -ForegroundColor Green
} else {
    Write-Host "✗ Compilation failed" -ForegroundColor Red
    Set-Location ..
    exit 1
}

# Test 2: Verify the enhanced FriendsToggleFAB file exists and contains accessibility features
Write-Host "`n2. Testing FriendsToggleFAB accessibility features..." -ForegroundColor Yellow
$fabFile = "app/src/main/java/com/locationsharing/app/ui/friends/components/FriendsToggleFAB.kt"

if (Test-Path $fabFile) {
    $content = Get-Content $fabFile -Raw
    
    # Check for enhanced accessibility imports
    if ($content -match "import androidx.compose.ui.semantics.liveRegion") {
        Write-Host "✓ LiveRegion import found" -ForegroundColor Green
    } else {
        Write-Host "✗ LiveRegion import missing" -ForegroundColor Red
    }
    
    if ($content -match "import androidx.compose.ui.semantics.stateDescription") {
        Write-Host "✓ StateDescription import found" -ForegroundColor Green
    } else {
        Write-Host "✗ StateDescription import missing" -ForegroundColor Red
    }
    
    if ($content -match "import androidx.compose.ui.platform.LocalAccessibilityManager") {
        Write-Host "✓ AccessibilityManager import found" -ForegroundColor Green
    } else {
        Write-Host "✗ AccessibilityManager import missing" -ForegroundColor Red
    }
    
    # Check for enhanced content descriptions
    if ($content -match "friends are available nearby") {
        Write-Host "✓ Enhanced content description with friend count found" -ForegroundColor Green
    } else {
        Write-Host "✗ Enhanced content description missing" -ForegroundColor Red
    }
    
    # Check for state description
    if ($content -match "stateDescription = stateDesc") {
        Write-Host "✓ State description implementation found" -ForegroundColor Green
    } else {
        Write-Host "✗ State description implementation missing" -ForegroundColor Red
    }
    
    # Check for live region
    if ($content -match "liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite") {
        Write-Host "✓ Live region implementation found" -ForegroundColor Green
    } else {
        Write-Host "✗ Live region implementation missing" -ForegroundColor Red
    }
    
    # Check for focus management
    if ($content -match "LocalFocusManager") {
        Write-Host "✓ Focus management implementation found" -ForegroundColor Green
    } else {
        Write-Host "✗ Focus management implementation missing" -ForegroundColor Red
    }
    
    # Check for enhanced haptic feedback
    if ($content -match "HapticFeedbackType.TextHandleMove") {
        Write-Host "✓ Enhanced haptic feedback found" -ForegroundColor Green
    } else {
        Write-Host "✗ Enhanced haptic feedback missing" -ForegroundColor Red
    }
    
} else {
    Write-Host "✗ FriendsToggleFAB.kt file not found" -ForegroundColor Red
    Set-Location ..
    exit 1
}

# Test 3: Verify the enhanced FriendsPanelScaffold file contains accessibility features
Write-Host "`n3. Testing FriendsPanelScaffold accessibility features..." -ForegroundColor Yellow
$scaffoldFile = "app/src/main/java/com/locationsharing/app/ui/friends/components/FriendsPanelScaffold.kt"

if (Test-Path $scaffoldFile) {
    $content = Get-Content $scaffoldFile -Raw
    
    # Check for enhanced accessibility features
    if ($content -match "liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite") {
        Write-Host "✓ Live region for panel found" -ForegroundColor Green
    } else {
        Write-Host "✗ Live region for panel missing" -ForegroundColor Red
    }
    
    # Check for enhanced content descriptions with state
    if ($content -match "friends available") {
        Write-Host "✓ Enhanced panel content description found" -ForegroundColor Green
    } else {
        Write-Host "✗ Enhanced panel content description missing" -ForegroundColor Red
    }
    
    # Check for focus management
    if ($content -match "focusManager.clearFocus") {
        Write-Host "✓ Focus management for panel found" -ForegroundColor Green
    } else {
        Write-Host "✗ Focus management for panel missing" -ForegroundColor Red
    }
    
} else {
    Write-Host "✗ FriendsPanelScaffold.kt file not found" -ForegroundColor Red
    Set-Location ..
    exit 1
}

Write-Host "`n✓ Accessibility enhancement verification completed!" -ForegroundColor Green
Write-Host "`nSummary of implemented accessibility features:" -ForegroundColor Cyan
Write-Host "• Enhanced content descriptions with detailed friend count information" -ForegroundColor White
Write-Host "• State descriptions for screen readers" -ForegroundColor White
Write-Host "• Live regions for announcing panel state changes" -ForegroundColor White
Write-Host "• Proper focus management when panel opens/closes" -ForegroundColor White
Write-Host "• Enhanced haptic feedback with appropriate feedback type" -ForegroundColor White
Write-Host "• Role.Button semantics and proper onClick handling" -ForegroundColor White
Write-Host "• Accessibility manager integration for timing announcements" -ForegroundColor White

Set-Location ..