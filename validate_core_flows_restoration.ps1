#!/usr/bin/env pwsh

# FFinder Core Flows Restoration Validation Script
# Tests all restored functionality on MapScreen and HomeScreen

Write-Host "🔧 FFinder Core Flows Restoration Validation" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# Set error handling
$ErrorActionPreference = "Continue"

# Build the project first
Write-Host "`n📦 Building FFinder project..." -ForegroundColor Yellow
Set-Location android
try {
    ./gradlew assembleDebug --quiet
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Build successful" -ForegroundColor Green
    } else {
        Write-Host "❌ Build failed" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Build error: $_" -ForegroundColor Red
    exit 1
}

# Check if emulator is running
Write-Host "`n📱 Checking emulator status..." -ForegroundColor Yellow
$emulatorCheck = adb devices | Select-String "emulator"
if (-not $emulatorCheck) {
    Write-Host "❌ No emulator detected. Please start an emulator first." -ForegroundColor Red
    Write-Host "   Run: emulator -avd `<your_avd_name`>" -ForegroundColor Yellow
    exit 1
}
Write-Host "✅ Emulator detected" -ForegroundColor Green

# Install the app
Write-Host "`n📲 Installing FFinder app..." -ForegroundColor Yellow
try {
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ App installed successfully" -ForegroundColor Green
    } else {
        Write-Host "❌ App installation failed" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Installation error: $_" -ForegroundColor Red
    exit 1
}

# Launch the app
Write-Host "`n🚀 Launching FFinder app..." -ForegroundColor Yellow
adb shell am start -n com.locationsharing.app.debug/com.locationsharing.app.MainActivity
Start-Sleep -Seconds 3

# Clear logcat and start monitoring
Write-Host "`n📋 Starting logcat monitoring..." -ForegroundColor Yellow
Write-Host "   Filtering for FFinder logs..." -ForegroundColor Gray

# Start logcat in background
$logcatJob = Start-Job -ScriptBlock {
    adb logcat -c  # Clear existing logs
    adb logcat | Select-String -Pattern "(FFinder|MapScreen|HomeScreen|QuickShare|DebugFAB|SelfLocation|NearbyFriends)"
}

Write-Host "`n🧪 TESTING CORE FLOWS" -ForegroundColor Magenta
Write-Host "=====================" -ForegroundColor Magenta

Write-Host "`n1️⃣ Testing HomeScreen 'Start Live Sharing' button..." -ForegroundColor Yellow
Write-Host "   Please tap the 'Start Live Sharing' button on the home screen" -ForegroundColor Gray
Write-Host "   Expected: Navigation to MapScreen + location sharing starts" -ForegroundColor Gray
Read-Host "   Press Enter after testing"

Write-Host "`n2️⃣ Testing MapScreen Quick Share FAB..." -ForegroundColor Yellow
Write-Host "   Please tap the Quick Share FAB (bottom-right, pin icon)" -ForegroundColor Gray
Write-Host "   Expected: System share intent opens with location data" -ForegroundColor Gray
Read-Host "   Press Enter after testing"

Write-Host "`n3️⃣ Testing Self-Location FAB..." -ForegroundColor Yellow
Write-Host "   Please tap the Self-Location FAB (above Quick Share FAB)" -ForegroundColor Gray
Write-Host "   Expected: Map camera centers on your current location" -ForegroundColor Gray
Read-Host "   Press Enter after testing"

Write-Host "`n4️⃣ Testing Nearby Friends Toggle..." -ForegroundColor Yellow
Write-Host "   Please tap the Nearby Friends button in the top app bar" -ForegroundColor Gray
Write-Host "   Expected: Drawer opens from right side showing friends list" -ForegroundColor Gray
Read-Host "   Press Enter after testing"

Write-Host "`n5️⃣ Testing Debug FAB (Debug builds only)..." -ForegroundColor Yellow
Write-Host "   Please tap the Debug FAB (bottom-left, purple flask icon)" -ForegroundColor Gray
Write-Host "   Expected: 5 mock friends added to map + snackbar message" -ForegroundColor Gray
Read-Host "   Press Enter after testing"

Write-Host "`n6️⃣ Testing Navigation Back Stack..." -ForegroundColor Yellow
Write-Host "   Please press the back button from MapScreen" -ForegroundColor Gray
Write-Host "   Expected: Returns to HomeScreen" -ForegroundColor Gray
Read-Host "   Press Enter after testing"

Write-Host "`n7️⃣ Testing Friends and Settings Navigation..." -ForegroundColor Yellow
Write-Host "   Please tap Friends and Settings buttons on HomeScreen" -ForegroundColor Gray
Write-Host "   Expected: Proper navigation to respective screens" -ForegroundColor Gray
Read-Host "   Press Enter after testing"

# Stop logcat monitoring
Write-Host "`n📋 Stopping logcat monitoring..." -ForegroundColor Yellow
Stop-Job $logcatJob
$logcatOutput = Receive-Job $logcatJob
Remove-Job $logcatJob

# Save logcat output
$logFile = "ffinder_core_flows_test_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"
$logcatOutput | Out-File -FilePath $logFile -Encoding UTF8

Write-Host "`n📊 VALIDATION RESULTS" -ForegroundColor Magenta
Write-Host "=====================" -ForegroundColor Magenta

# Analyze logcat for key events
$shareEvents = $logcatOutput | Select-String -Pattern "Quick share|ShareLocationUseCase"
$locationEvents = $logcatOutput | Select-String -Pattern "Self location center|Location updated"
$nearbyEvents = $logcatOutput | Select-String -Pattern "Nearby friends toggle|Drawer"
$debugEvents = $logcatOutput | Select-String -Pattern "Debug.*friends|addTestFriendsOnMap"
$navigationEvents = $logcatOutput | Select-String -Pattern "NavigationManager|navigateToMap|navigateToHome"

Write-Host "`n✅ DETECTED EVENTS:" -ForegroundColor Green
if ($shareEvents) {
    Write-Host "   📍 Quick Share events: $($shareEvents.Count)" -ForegroundColor Green
    $shareEvents | ForEach-Object { Write-Host "      $($_.Line)" -ForegroundColor Gray }
}

if ($locationEvents) {
    Write-Host "   🎯 Self Location events: $($locationEvents.Count)" -ForegroundColor Green
    $locationEvents | ForEach-Object { Write-Host "      $($_.Line)" -ForegroundColor Gray }
}

if ($nearbyEvents) {
    Write-Host "   👥 Nearby Friends events: $($nearbyEvents.Count)" -ForegroundColor Green
    $nearbyEvents | ForEach-Object { Write-Host "      $($_.Line)" -ForegroundColor Gray }
}

if ($debugEvents) {
    Write-Host "   🧪 Debug events: $($debugEvents.Count)" -ForegroundColor Green
    $debugEvents | ForEach-Object { Write-Host "      $($_.Line)" -ForegroundColor Gray }
}

if ($navigationEvents) {
    Write-Host "   🧭 Navigation events: $($navigationEvents.Count)" -ForegroundColor Green
    $navigationEvents | ForEach-Object { Write-Host "      $($_.Line)" -ForegroundColor Gray }
}

# Check for errors
$errorEvents = $logcatOutput | Select-String -Pattern "ERROR|Exception|Failed|Error"
if ($errorEvents) {
    Write-Host "`n❌ DETECTED ERRORS:" -ForegroundColor Red
    $errorEvents | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
}

Write-Host "`n📄 Full logcat saved to: $logFile" -ForegroundColor Cyan

# Summary
Write-Host "`n📋 VALIDATION SUMMARY" -ForegroundColor Magenta
Write-Host "=====================" -ForegroundColor Magenta

$totalEvents = ($shareEvents.Count + $locationEvents.Count + $nearbyEvents.Count + $debugEvents.Count + $navigationEvents.Count)
$hasErrors = $errorEvents.Count -gt 0

if ($totalEvents -gt 0 -and -not $hasErrors) {
    Write-Host "✅ CORE FLOWS RESTORATION: SUCCESS" -ForegroundColor Green
    Write-Host "   All major flows appear to be working correctly" -ForegroundColor Green
} elseif ($totalEvents -gt 0 -and $hasErrors) {
    Write-Host "⚠️  CORE FLOWS RESTORATION: PARTIAL SUCCESS" -ForegroundColor Yellow
    Write-Host "   Some flows working but errors detected" -ForegroundColor Yellow
} else {
    Write-Host "❌ CORE FLOWS RESTORATION: FAILED" -ForegroundColor Red
    Write-Host "   No expected events detected" -ForegroundColor Red
}

Write-Host "`n🎯 KEY METRICS:" -ForegroundColor Cyan
Write-Host "   📍 Quick Share events: $($shareEvents.Count)" -ForegroundColor White
Write-Host "   🎯 Self Location events: $($locationEvents.Count)" -ForegroundColor White
Write-Host "   👥 Nearby Friends events: $($nearbyEvents.Count)" -ForegroundColor White
Write-Host "   🧪 Debug events: $($debugEvents.Count)" -ForegroundColor White
Write-Host "   🧭 Navigation events: $($navigationEvents.Count)" -ForegroundColor White
Write-Host "   ❌ Error events: $($errorEvents.Count)" -ForegroundColor White

Write-Host "`n🔧 Core Flows Restoration Validation Complete!" -ForegroundColor Cyan

# Return to root directory
Set-Location ..