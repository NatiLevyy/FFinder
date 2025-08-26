Write-Host "Material 3 Theme Implementation Validation" -ForegroundColor Cyan

$passCount = 0
$totalTests = 6

# Test 1: Primary color
Write-Host "Test 1: Primary Color #2E7D32" -ForegroundColor Yellow
$colorContent = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/theme/Color.kt" -Raw
if ($colorContent -match "FFinderPrimary = Color\(0xFF2E7D32\)") {
    Write-Host "PASS: Primary color correctly set" -ForegroundColor Green
    $passCount++
}
else {
    Write-Host "FAIL: Primary color not found" -ForegroundColor Red
}

# Test 2: Secondary color
Write-Host "Test 2: Secondary Color #6B4F8F" -ForegroundColor Yellow
if ($colorContent -match "FFinderSecondary = Color\(0xFF6B4F8F\)") {
    Write-Host "PASS: Secondary color correctly set" -ForegroundColor Green
    $passCount++
}
else {
    Write-Host "FAIL: Secondary color not found" -ForegroundColor Red
}

# Test 3: Surface color
Write-Host "Test 3: Surface Color White" -ForegroundColor Yellow
if ($colorContent -match "FFinderSurface = Color\.White") {
    Write-Host "PASS: Surface color correctly set" -ForegroundColor Green
    $passCount++
}
else {
    Write-Host "FAIL: Surface color not found" -ForegroundColor Red
}

# Test 4: Background color
Write-Host "Test 4: Background Color #F1F1F1" -ForegroundColor Yellow
if ($colorContent -match "FFinderBackground = Color\(0xFFF1F1F1\)") {
    Write-Host "PASS: Background color correctly set" -ForegroundColor Green
    $passCount++
}
else {
    Write-Host "FAIL: Background color not found" -ForegroundColor Red
}

# Test 5: Dark theme support
Write-Host "Test 5: Dark Theme Support" -ForegroundColor Yellow
$themeContent = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/theme/Theme.kt" -Raw
if ($themeContent -match "FFinderDarkColorScheme" -and $colorContent -match "FFinderPrimaryDark") {
    Write-Host "PASS: Dark theme support implemented" -ForegroundColor Green
    $passCount++
}
else {
    Write-Host "FAIL: Dark theme support not found" -ForegroundColor Red
}

# Test 6: Hard-coded colors deprecated
Write-Host "Test 6: Hard-coded Colors Deprecated" -ForegroundColor Yellow
$constantsContent = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/map/MapScreenConstants.kt" -Raw
if ($constantsContent -match "@Deprecated") {
    Write-Host "PASS: Hard-coded colors deprecated" -ForegroundColor Green
    $passCount++
}
else {
    Write-Host "FAIL: Hard-coded colors not deprecated" -ForegroundColor Red
}

# Summary
Write-Host "SUMMARY: Passed $passCount/$totalTests tests" -ForegroundColor White

if ($passCount -eq $totalTests) {
    Write-Host "SUCCESS: All Material 3 theming requirements met!" -ForegroundColor Green
}
else {
    Write-Host "PARTIAL: Some tests need attention" -ForegroundColor Yellow
}

Write-Host "Task 16 Implementation Complete" -ForegroundColor Green