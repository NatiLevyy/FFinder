#!/bin/bash

# FFinder Headless Build & Test Script
# Builds debug APK, creates emulator, installs app, and runs smoke tests

set -e  # Exit on any error

echo "🚀 Starting FFinder headless build and test..."

# Navigate to android directory
cd android

# Step 1: Clean project
echo "📦 Cleaning project..."
./gradlew clean

# Step 2: Assemble debug APK
echo "🔨 Building debug APK..."
./gradlew :app:assembleDebug --no-daemon --stacktrace

# Step 3: Create & boot headless AVD (if not existing)
echo "📱 Setting up Android emulator..."
sdkmanager "system-images;android-34;google_apis;x86_64" --licenses
avdmanager create avd -n ff_emulator -k "system-images;android-34;google_apis;x86_64" --device "pixel_5" --force

echo "🚀 Starting headless emulator..."
emulator -avd ff_emulator -no-window -no-audio -no-boot-anim &
EMULATOR_PID=$!

# Step 4: Wait for device & disable animations
echo "⏳ Waiting for device to boot..."
adb wait-for-device
echo "🎬 Disabling animations..."
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

# Step 5: Install APK
echo "📲 Installing APK..."
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
adb install -r "$APK_PATH"

# Step 6: Launch main activity
echo "🎯 Launching MainActivity..."
adb shell am start -n "com.locationsharing.app.debug/.MainActivity"

# Step 7: Stream logcat to file
echo "📝 Starting logcat capture..."
mkdir -p build
adb logcat -c
adb logcat > build/ffinder_logcat.txt &
LOGCAT_PID=$!

# Wait a moment for app to start
sleep 5

# Check if MainActivity launched successfully
echo "🔍 Checking if MainActivity launched..."
if adb shell dumpsys activity activities | grep -q "com.locationsharing.app.debug/.MainActivity"; then
    echo "✅ MainActivity launched successfully!"
    LAUNCH_SUCCESS=true
else
    echo "❌ MainActivity failed to launch!"
    LAUNCH_SUCCESS=false
fi

# Output summary
echo ""
echo "📊 BUILD & TEST SUMMARY"
echo "======================"
echo "APK Path: $(pwd)/$APK_PATH"
echo "Logcat File: $(pwd)/build/ffinder_logcat.txt"
echo "Emulator: ff_emulator (API 34, Pixel 5)"
echo "MainActivity Launch: $([ "$LAUNCH_SUCCESS" = true ] && echo "✅ SUCCESS" || echo "❌ FAILED")"
echo ""

# Check for crashes in logcat
echo "🔍 Checking for crashes..."
sleep 2
if grep -q "FATAL EXCEPTION\|AndroidRuntime" build/ffinder_logcat.txt; then
    echo "⚠️  CRASH DETECTED! Stack traces:"
    echo "================================="
    grep -A 10 "FATAL EXCEPTION\|AndroidRuntime" build/ffinder_logcat.txt
else
    echo "✅ No crashes detected in initial logcat"
fi

echo ""
echo "📖 HOW TO REBUILD & RUN LOCALLY"
echo "==============================="
echo "1. Clean: ./gradlew clean"
echo "2. Build: ./gradlew :app:assembleDebug --no-daemon --stacktrace"
echo "3. Setup emulator: avdmanager create avd -n ff_emulator -k 'system-images;android-34;google_apis;x86_64' --device 'pixel_5' --force"
echo "4. Start emulator: emulator -avd ff_emulator -no-window -no-audio -no-boot-anim &"
echo "5. Wait & setup: adb wait-for-device && adb shell settings put global window_animation_scale 0"
echo "6. Install: adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo "7. Launch: adb shell am start -n 'com.locationsharing.app.debug/.MainActivity'"
echo "8. Monitor: adb logcat > build/ffinder_logcat.txt &"
echo ""
echo "🎉 Script completed! Emulator PID: $EMULATOR_PID, Logcat PID: $LOGCAT_PID"
echo "💡 To stop: kill $EMULATOR_PID $LOGCAT_PID"