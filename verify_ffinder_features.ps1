#!/usr/bin/env pwsh

# FFinder Features Verification Script
# This script rebuilds, reinstalls, and verifies the Nearby Friends Panel and Debug Friends features

Write-Host "🔧 FFinder Features Verification Script" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Green

# Step 1: Clean and rebuild
Write-Host "`n1. Cleaning and rebuilding the app..." -ForegroundColor Yellow
Set-Location android
& ./gradlew clean :app:assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Build successful!" -ForegroundColor Green

# Step 2: Uninstall existing app
Write-Host "`n2. Uninstalling existing app..." -ForegroundColor Yellow
& adb uninstall com.locationsharing.app.debug 2>$null
Write-Host "✅ App uninstalled (if it existed)" -ForegroundColor Green

# Step 3: Install new APK
Write-Host "`n3. Installing new APK..." -ForegroundColor Yellow
& adb install app/build/outputs/apk/debug/app-debug.apk

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Installation failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ App installed successfully!" -ForegroundColor Green

# Step 4: Clear logcat and launch app
Write-Host "`n4. Clearing logcat and launching app..." -ForegroundColor Yellow
& adb logcat -c
& adb shell am start -n com.locationsharing.app.debug/com.locationsharing.app.MainActivity

Start-Sleep -Seconds 2
Write-Host "✅ App launched!" -ForegroundColor Green

# Step 5: Monitor logcat for feature verification
Write-Host "`n5. Monitoring logcat for 30 seconds..." -ForegroundColor Yellow
Write-Host "Look for the following features:" -ForegroundColor Cyan
Write-Host "  • Friends Toggle FAB (top-right corner)" -ForegroundColor Cyan
Write-Host "  • Debug Friends Button (bottom-left, science icon)" -ForegroundColor Cyan
Write-Host "  • Nearby Friends Panel (swipe from right or tap FAB)" -ForegroundColor Cyan

Write-Host "`nLogcat output:" -ForegroundColor White
Write-Host "==============" -ForegroundColor White

# Monitor logcat for 30 seconds
$job = Start-Job -ScriptBlock {
    & adb logcat -s "FFinder" -s "NearbyPanel" -s "FriendsMap" -s "System.err" -s "AndroidRuntime"
}

Start-Sleep -Seconds 30
Stop-Job $job
Receive-Job $job
Remove-Job $job

Write-Host "`n🎉 Verification complete!" -ForegroundColor Green
Write-Host "Features to test manually:" -ForegroundColor Yellow
Write-Host "1. Tap the Friends FAB (top-right) to open/close the Nearby Friends panel" -ForegroundColor White
Write-Host "2. Tap the Debug button (bottom-left, science icon) to add 5 dummy friends" -ForegroundColor White
Write-Host "3. Verify the panel shows the dummy friends with distances" -ForegroundColor White

Set-Location ..