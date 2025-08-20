# FFinder Headless Build & Test Script (PowerShell)
# Builds debug APK, creates emulator, installs app, and runs smoke tests

Write-Host "Starting FFinder headless build and test..." -ForegroundColor Green

# Navigate to android directory
Set-Location android

# Step 1: Clean project
Write-Host "Cleaning project..." -ForegroundColor Yellow
.\gradlew clean

# Step 2: Assemble debug APK
Write-Host "Building debug APK..." -ForegroundColor Yellow
.\gradlew :app:assembleDebug --no-daemon --stacktrace

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# Step 3: Create & boot headless AVD (if not existing)
Write-Host "Setting up Android emulator..." -ForegroundColor Yellow

# Accept licenses
sdkmanager --licenses

# Install system image
sdkmanager "system-images;android-34;google_apis;x86_64"

# Create AVD
avdmanager create avd -n ff_emulator -k "system-images;android-34;google_apis;x86_64" --device "pixel_5" --force

Write-Host "Starting headless emulator..." -ForegroundColor Yellow
$emulatorProcess = Start-Process -FilePath "emulator" -ArgumentList "-avd", "ff_emulator", "-no-window", "-no-audio", "-no-boot-anim" -PassThru

# Step 4: Wait for device & disable animations
Write-Host "Waiting for device to boot..." -ForegroundColor Yellow
adb wait-for-device

Write-Host "Disabling animations..." -ForegroundColor Yellow
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

# Step 5: Install APK
Write-Host "Installing APK..." -ForegroundColor Yellow
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
adb install -r $apkPath

if ($LASTEXITCODE -ne 0) {
    Write-Host "APK installation failed!" -ForegroundColor Red
    exit 1
}

# Step 6: Launch main activity
Write-Host "Launching MainActivity..." -ForegroundColor Yellow
adb shell am start -n "com.locationsharing.app.debug/.MainActivity"

# Step 7: Stream logcat to file
Write-Host "Starting logcat capture..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "build"
adb logcat -c
$logcatProcess = Start-Process -FilePath "adb" -ArgumentList "logcat" -RedirectStandardOutput "build\ffinder_logcat.txt" -PassThru

# Wait a moment for app to start
Start-Sleep -Seconds 5

# Check if MainActivity launched successfully
Write-Host "Checking if MainActivity launched..." -ForegroundColor Yellow
$activityCheck = adb shell dumpsys activity activities | Select-String "com.locationsharing.app.debug/.MainActivity"

if ($activityCheck) {
    Write-Host "MainActivity launched successfully!" -ForegroundColor Green
    $launchSuccess = $true
} else {
    Write-Host "MainActivity failed to launch!" -ForegroundColor Red
    $launchSuccess = $false
}

# Output summary
Write-Host ""
Write-Host "BUILD AND TEST SUMMARY" -ForegroundColor Cyan
Write-Host "=====================" -ForegroundColor Cyan
Write-Host "APK Path: $(Get-Location)\$apkPath"
Write-Host "Logcat File: $(Get-Location)\build\ffinder_logcat.txt"
Write-Host "Emulator: ff_emulator (API 34, Pixel 5)"
if ($launchSuccess) {
    Write-Host "MainActivity Launch: SUCCESS" -ForegroundColor Green
} else {
    Write-Host "MainActivity Launch: FAILED" -ForegroundColor Red
}
Write-Host ""

# Check for crashes in logcat
Write-Host "Checking for crashes..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

if (Test-Path "build\ffinder_logcat.txt") {
    $crashLines = Select-String -Path "build\ffinder_logcat.txt" -Pattern "FATAL EXCEPTION|AndroidRuntime"
    if ($crashLines) {
        Write-Host "CRASH DETECTED! Stack traces:" -ForegroundColor Red
        Write-Host "==============================" -ForegroundColor Red
        $crashLines | ForEach-Object { Write-Host $_.Line }
    } else {
        Write-Host "No crashes detected in initial logcat" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "HOW TO REBUILD AND RUN LOCALLY" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan
Write-Host "1. Clean: .\gradlew clean"
Write-Host "2. Build: .\gradlew :app:assembleDebug --no-daemon --stacktrace"
Write-Host "3. Setup emulator: avdmanager create avd -n ff_emulator -k 'system-images;android-34;google_apis;x86_64' --device 'pixel_5' --force"
Write-Host "4. Start emulator: emulator -avd ff_emulator -no-window -no-audio -no-boot-anim"
Write-Host "5. Wait and setup: adb wait-for-device; adb shell settings put global window_animation_scale 0"
Write-Host "6. Install: adb install -r app\build\outputs\apk\debug\app-debug.apk"
Write-Host "7. Launch: adb shell am start -n 'com.locationsharing.app.debug/.MainActivity'"
Write-Host "8. Monitor: adb logcat > build\ffinder_logcat.txt"
Write-Host ""
Write-Host "Script completed! Emulator PID: $($emulatorProcess.Id), Logcat PID: $($logcatProcess.Id)" -ForegroundColor Green
Write-Host "To stop: Stop-Process -Id $($emulatorProcess.Id), $($logcatProcess.Id)" -ForegroundColor Yellow