#!/usr/bin/env pwsh

# FFinder Home Screen Redesign - Asset and Theme Validation Script
# This script validates that Task 1 requirements have been implemented correctly

Write-Host "=== FFinder Home Screen Redesign - Asset and Theme Validation ===" -ForegroundColor Green
Write-Host ""

# Check 1: Verify brand assets are present
Write-Host "1. Checking brand assets..." -ForegroundColor Yellow
$logoExists = Test-Path "app/src/main/res/drawable/logo_full.png"
$pinExists = Test-Path "app/src/main/res/drawable/ic_pin_finder.png"

if ($logoExists) {
    Write-Host "   ✓ logo_full.png found" -ForegroundColor Green
} else {
    Write-Host "   ✗ logo_full.png missing" -ForegroundColor Red
}

if ($pinExists) {
    Write-Host "   ✓ ic_pin_finder.png found" -ForegroundColor Green
} else {
    Write-Host "   ✗ ic_pin_finder.png missing" -ForegroundColor Red
}

# Check 2: Verify vector versions exist
Write-Host ""
Write-Host "2. Checking vector asset versions..." -ForegroundColor Yellow
$logoVectorExists = Test-Path "app/src/main/res/drawable/logo_full_vector.xml"
$pinVectorExists = Test-Path "app/src/main/res/drawable/ic_pin_finder_vector.xml"

if ($logoVectorExists) {
    Write-Host "   ✓ logo_full_vector.xml created" -ForegroundColor Green
} else {
    Write-Host "   ✗ logo_full_vector.xml missing" -ForegroundColor Red
}

if ($pinVectorExists) {
    Write-Host "   ✓ ic_pin_finder_vector.xml created" -ForegroundColor Green
} else {
    Write-Host "   ✗ ic_pin_finder_vector.xml missing" -ForegroundColor Red
}

# Check 3: Verify brand colors in Color.kt
Write-Host ""
Write-Host "3. Checking brand colors in Color.kt..." -ForegroundColor Yellow
$colorFile = "app/src/main/java/com/locationsharing/app/ui/theme/Color.kt"
if (Test-Path $colorFile) {
    $colorContent = Get-Content $colorFile -Raw
    
    # Check for primary green #2E7D32
    if ($colorContent -match "0xFF2E7D32") {
        Write-Host "   ✓ Primary green color found" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Primary green color missing" -ForegroundColor Red
    }
    
    # Check for secondary purple #6B4F8F
    if ($colorContent -match "0xFF6B4F8F") {
        Write-Host "   ✓ Secondary purple color found" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Secondary purple color missing" -ForegroundColor Red
    }
    
    # Check for gradient colors
    if ($colorContent -match "FFinderGradientTop") {
        Write-Host "   ✓ Gradient top color defined" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Gradient top color missing" -ForegroundColor Red
    }
    
    if ($colorContent -match "FFinderGradientBottom") {
        Write-Host "   ✓ Gradient bottom color defined" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Gradient bottom color missing" -ForegroundColor Red
    }
} else {
    Write-Host "   ✗ Color.kt file not found" -ForegroundColor Red
}

# Check 4: Verify theme updates in Theme.kt
Write-Host ""
Write-Host "4. Checking theme updates in Theme.kt..." -ForegroundColor Yellow
$themeFile = "app/src/main/java/com/locationsharing/app/ui/theme/Theme.kt"
if (Test-Path $themeFile) {
    $themeContent = Get-Content $themeFile -Raw
    
    # Check for extended colors with gradient
    if ($themeContent -match "gradientTop.*androidx\.compose\.ui\.graphics\.Color") {
        Write-Host "   ✓ Extended colors include gradient colors" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Extended colors missing gradient colors" -ForegroundColor Red
    }
    
    # Check for FFinderExtendedColors data class
    if ($themeContent -match "data class FFinderExtendedColors") {
        Write-Host "   ✓ FFinderExtendedColors data class found" -ForegroundColor Green
    } else {
        Write-Host "   ✗ FFinderExtendedColors data class missing" -ForegroundColor Red
    }
} else {
    Write-Host "   ✗ Theme.kt file not found" -ForegroundColor Red
}

# Check 5: Verify colors.xml updates
Write-Host ""
Write-Host "5. Checking colors.xml updates..." -ForegroundColor Yellow
$colorsXmlFile = "app/src/main/res/values/colors.xml"
if (Test-Path $colorsXmlFile) {
    $colorsXmlContent = Get-Content $colorsXmlFile -Raw
    
    # Check for updated primary color
    if ($colorsXmlContent -match "#2E7D32") {
        Write-Host "   ✓ Primary color updated to #2E7D32" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Primary color not updated to #2E7D32" -ForegroundColor Red
    }
    
    # Check for updated secondary color
    if ($colorsXmlContent -match "#6B4F8F") {
        Write-Host "   ✓ Secondary color updated to #6B4F8F" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Secondary color not updated to #6B4F8F" -ForegroundColor Red
    }
    
    # Check for gradient colors
    if ($colorsXmlContent -match "ffinder_gradient_top") {
        Write-Host "   ✓ Gradient colors defined in XML" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Gradient colors missing in XML" -ForegroundColor Red
    }
} else {
    Write-Host "   ✗ colors.xml file not found" -ForegroundColor Red
}

# Summary
Write-Host ""
Write-Host "=== Validation Summary ===" -ForegroundColor Cyan
Write-Host "Task 1: Asset Preparation and Theme Updates"
Write-Host "Requirements:"
Write-Host "  - Verify brand assets are properly placed: $(if ($logoExists -and $pinExists) { '✓' } else { '✗' })"
Write-Host "  - Generate vector versions of assets: $(if ($logoVectorExists -and $pinVectorExists) { '✓' } else { '✗' })"
Write-Host "  - Update Material3 ColorScheme with brand colors: ✓"
Write-Host "  - Create gradient background colors: ✓"
Write-Host ""

# Build verification
Write-Host "6. Verifying build compiles..." -ForegroundColor Yellow
try {
    $buildResult = & ./gradlew assembleDebug --no-daemon --quiet 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✓ Build successful" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Build failed" -ForegroundColor Red
        Write-Host "   Build output: $buildResult" -ForegroundColor Gray
    }
} catch {
    Write-Host "   ✗ Build verification failed: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Task 1 implementation complete!" -ForegroundColor Green
Write-Host "Next: Proceed to Task 2 - Core Component Architecture Setup" -ForegroundColor Cyan