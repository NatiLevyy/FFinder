#!/usr/bin/env pwsh

# Material 3 Theme Implementation Validation Script
# Validates that task 16 requirements are met

Write-Host "🎨 Material 3 Theme Implementation Validation" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$validationResults = @()

# Function to add validation result
function Add-ValidationResult {
    param($Test, $Status, $Details)
    $validationResults += [PSCustomObject]@{
        Test = $Test
        Status = $Status
        Details = $Details
    }
}

# Test 1: Verify primary color matches requirement 10.1 (#2E7D32)
Write-Host "`n📋 Test 1: Primary Color Requirement 10.1" -ForegroundColor Yellow
$colorKtContent = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/theme/Color.kt" -Raw
if ($colorKtContent -match "FFinderPrimary = Color\(0xFF2E7D32\)") {
    Add-ValidationResult "Primary Color #2E7D32" "✅ PASS" "Found correct primary color definition"
    Write-Host "✅ Primary color correctly set to #2E7D32" -ForegroundColor Green
}
else {
    Add-ValidationResult "Primary Color #2E7D32" "❌ FAIL" "Primary color not found or incorrect"
    Write-Host "❌ Primary color not correctly set" -ForegroundColor Red
}

# Test 2: Verify secondary color matches requirement 10.2 (#6B4F8F)
Write-Host "`n📋 Test 2: Secondary Color Requirement 10.2" -ForegroundColor Yellow
if ($colorKtContent -match "FFinderSecondary = Color\(0xFF6B4F8F\)") {
    Add-ValidationResult "Secondary Color #6B4F8F" "✅ PASS" "Found correct secondary color definition"
    Write-Host "✅ Secondary color correctly set to #6B4F8F" -ForegroundColor Green
}
else {
    Add-ValidationResult "Secondary Color #6B4F8F" "❌ FAIL" "Secondary color not found or incorrect"
    Write-Host "❌ Secondary color not correctly set" -ForegroundColor Red
}

# Test 3: Verify surface color matches requirement 10.3 (White)
Write-Host "`n📋 Test 3: Surface Color Requirement 10.3" -ForegroundColor Yellow
if ($colorKtContent -match "FFinderSurface = Color\.White") {
    Add-ValidationResult "Surface Color White" "✅ PASS" "Found correct surface color definition"
    Write-Host "✅ Surface color correctly set to White" -ForegroundColor Green
} else {
    Add-ValidationResult "Surface Color White" "❌ FAIL" "Surface color not found or incorrect"
    Write-Host "❌ Surface color not correctly set" -ForegroundColor Red
}

# Test 4: Verify background color matches requirement 10.4 (#F1F1F1)
Write-Host "`n📋 Test 4: Background Color Requirement 10.4" -ForegroundColor Yellow
if ($colorKtContent -match "FFinderBackground = Color\(0xFFF1F1F1\)") {
    Add-ValidationResult "Background Color #F1F1F1" "✅ PASS" "Found correct background color definition"
    Write-Host "✅ Background color correctly set to #F1F1F1" -ForegroundColor Green
} else {
    Add-ValidationResult "Background Color #F1F1F1" "❌ FAIL" "Background color not found or incorrect"
    Write-Host "❌ Background color not correctly set" -ForegroundColor Red
}

# Test 5: Verify theme uses Material 3 color scheme
Write-Host "`n📋 Test 5: Material 3 Color Scheme Usage" -ForegroundColor Yellow
$themeKtContent = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/theme/Theme.kt" -Raw
if ($themeKtContent -match "lightColorScheme" -and $themeKtContent -match "darkColorScheme") {
    Add-ValidationResult "Material 3 ColorScheme" "✅ PASS" "Found lightColorScheme and darkColorScheme usage"
    Write-Host "✅ Theme uses Material 3 color schemes" -ForegroundColor Green
} else {
    Add-ValidationResult "Material 3 ColorScheme" "❌ FAIL" "Material 3 color schemes not found"
    Write-Host "❌ Theme does not use Material 3 color schemes" -ForegroundColor Red
}

# Test 6: Verify dark theme support (requirement 10.6)
Write-Host "`n📋 Test 6: Dark Theme Support Requirement 10.6" -ForegroundColor Yellow
if ($themeKtContent -match "FFinderDarkColorScheme" -and $colorKtContent -match "FFinderPrimaryDark") {
    Add-ValidationResult "Dark Theme Support" "✅ PASS" "Found dark theme color definitions"
    Write-Host "✅ Dark theme support implemented" -ForegroundColor Green
} else {
    Add-ValidationResult "Dark Theme Support" "❌ FAIL" "Dark theme colors not found"
    Write-Host "❌ Dark theme support not properly implemented" -ForegroundColor Red
}

# Test 7: Verify elevation and surface color handling
Write-Host "`n📋 Test 7: Elevation and Surface Color Handling" -ForegroundColor Yellow
if ($themeKtContent -match "surfaceVariant" -and $themeKtContent -match "surfaceTint") {
    Add-ValidationResult "Elevation Support" "✅ PASS" "Found Material 3 elevation colors"
    Write-Host "✅ Elevation and surface color handling implemented" -ForegroundColor Green
} else {
    Add-ValidationResult "Elevation Support" "❌ FAIL" "Material 3 elevation colors not found"
    Write-Host "❌ Elevation and surface color handling not implemented" -ForegroundColor Red
}

# Test 8: Verify hard-coded colors are removed/deprecated
Write-Host "`n📋 Test 8: Hard-coded Colors Removal" -ForegroundColor Yellow
$constantsContent = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/map/MapScreenConstants.kt" -Raw
if ($constantsContent -match "@Deprecated" -and $constantsContent -match "Use MaterialTheme.colorScheme") {
    Add-ValidationResult "Hard-coded Colors" "✅ PASS" "Found deprecated annotations on hard-coded colors"
    Write-Host "✅ Hard-coded colors properly deprecated" -ForegroundColor Green
} else {
    Add-ValidationResult "Hard-coded Colors" "⚠️ WARN" "Hard-coded colors may not be properly deprecated"
    Write-Host "⚠️ Hard-coded colors deprecation needs verification" -ForegroundColor Yellow
}

# Test 9: Verify components use theme colors
Write-Host "`n📋 Test 9: Component Theme Color Usage" -ForegroundColor Yellow
$componentFiles = @(
    "android/app/src/main/java/com/locationsharing/app/ui/map/components/CurrentLocationMarker.kt",
    "android/app/src/main/java/com/locationsharing/app/ui/map/components/DebugFAB.kt",
    "android/app/src/main/java/com/locationsharing/app/ui/map/components/NearbyFriendsDrawer.kt"
)

$themeUsageFound = $false
foreach ($file in $componentFiles) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        if ($content -match "MaterialTheme\.colorScheme") {
            $themeUsageFound = $true
            break
        }
    }
}

if ($themeUsageFound) {
    Add-ValidationResult "Component Theme Usage" "✅ PASS" "Found MaterialTheme.colorScheme usage in components"
    Write-Host "✅ Components use theme colors" -ForegroundColor Green
} else {
    Add-ValidationResult "Component Theme Usage" "❌ FAIL" "Components not using MaterialTheme.colorScheme"
    Write-Host "❌ Components not using theme colors" -ForegroundColor Red
}

# Test 10: Verify XML colors match requirements
Write-Host "`n📋 Test 10: XML Colors Consistency" -ForegroundColor Yellow
$colorsXmlContent = Get-Content "android/app/src/main/res/values/colors.xml" -Raw
$xmlColorsCorrect = ($colorsXmlContent -match "#2E7D32") -and 
                   ($colorsXmlContent -match "#6B4F8F") -and 
                   ($colorsXmlContent -match "#FFFFFF") -and 
                   ($colorsXmlContent -match "#F1F1F1")

if ($xmlColorsCorrect) {
    Add-ValidationResult "XML Colors" "✅ PASS" "XML colors match requirements"
    Write-Host "✅ XML colors match theme requirements" -ForegroundColor Green
} else {
    Add-ValidationResult "XML Colors" "❌ FAIL" "XML colors do not match requirements"
    Write-Host "❌ XML colors do not match requirements" -ForegroundColor Red
}

# Test 11: Verify compilation success
Write-Host "`n📋 Test 11: Compilation Verification" -ForegroundColor Yellow
try {
    $compileResult = & ./gradlew :app:compileDebugKotlin --console=plain 2>&1
    if ($LASTEXITCODE -eq 0) {
        Add-ValidationResult "Compilation" "✅ PASS" "Code compiles successfully"
        Write-Host "✅ Code compiles successfully" -ForegroundColor Green
    } else {
        Add-ValidationResult "Compilation" "❌ FAIL" "Compilation failed"
        Write-Host "❌ Compilation failed" -ForegroundColor Red
    }
} catch {
    Add-ValidationResult "Compilation" "❌ FAIL" "Could not run compilation test"
    Write-Host "❌ Could not verify compilation" -ForegroundColor Red
}

# Summary
Write-Host "`n📊 VALIDATION SUMMARY" -ForegroundColor Cyan
Write-Host "===================" -ForegroundColor Cyan

$passCount = ($validationResults | Where-Object { $_.Status -eq "✅ PASS" }).Count
$failCount = ($validationResults | Where-Object { $_.Status -eq "❌ FAIL" }).Count
$warnCount = ($validationResults | Where-Object { $_.Status -eq "⚠️ WARN" }).Count
$totalTests = $validationResults.Count

Write-Host "`nResults:" -ForegroundColor White
Write-Host "✅ Passed: $passCount/$totalTests" -ForegroundColor Green
Write-Host "❌ Failed: $failCount/$totalTests" -ForegroundColor Red
Write-Host "⚠️ Warnings: $warnCount/$totalTests" -ForegroundColor Yellow

# Detailed results
Write-Host "`nDetailed Results:" -ForegroundColor White
$validationResults | ForEach-Object {
    Write-Host "$($_.Status) $($_.Test): $($_.Details)" -ForegroundColor White
}

# Requirements mapping
Write-Host "`n📋 Requirements Compliance:" -ForegroundColor Cyan
Write-Host "- Requirement 10.1 (Primary #2E7D32): $(if ($validationResults | Where-Object { $_.Test -eq "Primary Color #2E7D32" -and $_.Status -eq "✅ PASS" }) { "✅ COMPLIANT" } else { "❌ NON-COMPLIANT" })"
Write-Host "- Requirement 10.2 (Secondary #6B4F8F): $(if ($validationResults | Where-Object { $_.Test -eq "Secondary Color #6B4F8F" -and $_.Status -eq "✅ PASS" }) { "✅ COMPLIANT" } else { "❌ NON-COMPLIANT" })"
Write-Host "- Requirement 10.3 (Surface White): $(if ($validationResults | Where-Object { $_.Test -eq "Surface Color White" -and $_.Status -eq "✅ PASS" }) { "✅ COMPLIANT" } else { "❌ NON-COMPLIANT" })"
Write-Host "- Requirement 10.4 (Background #F1F1F1): $(if ($validationResults | Where-Object { $_.Test -eq "Background Color #F1F1F1" -and $_.Status -eq "✅ PASS" }) { "✅ COMPLIANT" } else { "❌ NON-COMPLIANT" })"
Write-Host "- Requirement 10.5 (No hard-coded colors): $(if ($validationResults | Where-Object { $_.Test -eq "Hard-coded Colors" -and $_.Status -ne "❌ FAIL" }) { "✅ COMPLIANT" } else { "❌ NON-COMPLIANT" })"
Write-Host "- Requirement 10.6 (Dark theme support): $(if ($validationResults | Where-Object { $_.Test -eq "Dark Theme Support" -and $_.Status -eq "✅ PASS" }) { "✅ COMPLIANT" } else { "❌ NON-COMPLIANT" })"

# Overall status
if ($failCount -eq 0) {
    Write-Host "`n🎉 TASK 16 IMPLEMENTATION: SUCCESS" -ForegroundColor Green
    Write-Host "All Material 3 theming requirements have been met!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n⚠️ TASK 16 IMPLEMENTATION: NEEDS ATTENTION" -ForegroundColor Yellow
    Write-Host "$failCount test(s) failed. Please review and fix the issues above." -ForegroundColor Yellow
    exit 1
}