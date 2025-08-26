#!/usr/bin/env pwsh

# Test script to verify FFinder FABs functionality
Write-Host "🧪 Testing FFinder FABs functionality..." -ForegroundColor Green

# Clear logcat
Write-Host "📋 Clearing logcat..." -ForegroundColor Yellow
adb logcat -c

# Start the app
Write-Host "🚀 Starting FFinder app..." -ForegroundColor Yellow
adb shell am start -n com.locationsharing.app.debug/com.locationsharing.app.MainActivity

# Wait for app to load
Start-Sleep -Seconds 3

# Navigate to MapScreen (assuming it's accessible from main screen)
Write-Host "🗺️ Navigating to MapScreen..." -ForegroundColor Yellow
# Tap on the map or location button (coordinates may need adjustment)
adb shell input tap 500 1000

# Wait for MapScreen to load
Start-Sleep -Seconds 2

Write-Host "📱 Testing FAB interactions..." -ForegroundColor Yellow

# Test 1: Try to tap the Friends Toggle FAB (top-right corner)
Write-Host "🟢 Testing Friends Toggle FAB (top-right)..." -ForegroundColor Cyan
adb shell input tap 950 150  # Top-right corner where the green FAB should be

Start-Sleep -Seconds 2

# Test 2: Try to tap the Self-Location FAB (bottom-right corner)
Write-Host "📍 Testing Self-Location FAB (bottom-right)..." -ForegroundColor Cyan
adb shell input tap 950 1800  # Bottom-right corner where the self-location FAB should be

Start-Sleep -Seconds 2

# Test 3: Try to tap the Friends Toggle FAB again to close panel
Write-Host "🔴 Testing Friends Toggle FAB again (to close panel)..." -ForegroundColor Cyan
adb shell input tap 950 150

Start-Sleep -Seconds 2

# Capture logcat for analysis
Write-Host "📋 Capturing logcat for analysis..." -ForegroundColor Yellow
$logOutput = adb logcat -d | Select-String -Pattern "FAB|Panel|Nearby|Toggle|SelfLocation|MapScreen" | Select-Object -First 30

if ($logOutput) {
    Write-Host "✅ Found relevant log entries:" -ForegroundColor Green
    $logOutput | ForEach-Object { Write-Host $_.Line -ForegroundColor White }
} else {
    Write-Host "❌ No relevant log entries found. FABs may not be working correctly." -ForegroundColor Red
}

Write-Host "🏁 Test completed!" -ForegroundColor Green