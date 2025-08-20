# Running FFinder on Android Emulator

This document contains the exact commands to build and run the FFinder app on an Android emulator.

## Prerequisites

- Android Studio with SDK installed
- Android emulator running (e.g., Pixel_5 API 34)
- ADB tools available in PATH

## Build and Deploy Commands

Execute these commands from the `android` directory:

### 1. Navigate to Android Directory
```bash
cd android
```

### 2. Build Debug APK
```bash
./gradlew assembleDebug
```

### 3. Install Debug APK on Emulator
```bash
./gradlew installDebug
```

### 4. Launch the App
```bash
adb shell am start -n com.locationsharing.app.debug/com.locationsharing.app.MainActivity
```

## Verification Commands

### Check Installed Packages
```bash
adb shell pm list packages | findstr locationsharing
```
Expected output: `package:com.locationsharing.app.debug`

### Check App is Running
```bash
adb shell dumpsys activity activities | findstr locationsharing
```

## Troubleshooting

### If App Launch Fails
1. Verify the correct package name:
   ```bash
   adb shell pm list packages | findstr locationsharing
   ```

2. Check if emulator is running:
   ```bash
   adb devices
   ```

3. For debug builds, the package name includes `.debug` suffix

### Clean Build (if needed)
```bash
./gradlew clean
./gradlew assembleDebug
```

## Notes

- The debug build uses package name `com.locationsharing.app.debug`
- The release build would use `com.locationsharing.app`
- All animation performance optimizations are included in this build
- The app includes accessibility features and reduced motion alternatives

## Build Output

Successful build will show:
```
BUILD SUCCESSFUL in Xs
XX actionable tasks: X executed, XX up-to-date
```

Successful installation will show:
```
Installing APK 'app-debug.apk' on 'Pixel_5(AVD) - 16' for :app:debug
Installed on 1 device.
```

Successful app launch will show:
```
Starting: Intent { cmp=com.locationsharing.app.debug/com.locationsharing.app.MainActivity }
```