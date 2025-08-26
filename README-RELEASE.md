# FFinder Android Release Build Guide

This guide explains how to create a signed release APK for FFinder Android app.

## Prerequisites

- Android Studio installed
- Access to Firebase Console for the project
- Access to Google Cloud Console for Maps API

## Step 1: Create Release Keystore

### Option A: Quick Setup Script
Run the provided script to create your keystore:
- **Windows**: Double-click `create-release-keystore.bat`
- **Mac/Linux**: Run `./create-release-keystore.sh`

### Option B: Android Studio UI
1. Open Android Studio
2. Go to **Build** → **Generate Signed Bundle/APK**
3. Select **APK** → **Next**
4. Click **Create new...** under Key store path
5. Fill in keystore details:
   - **Key store path**: Choose location (e.g., `C:\path\to\ffinder-release.jks`)
   - **Password**: Create strong password
   - **Key alias**: `ffinder-release` (or your preferred alias)
   - **Key password**: Create strong password
   - **Validity**: 25+ years
   - **Certificate info**: Fill company/personal details
6. Click **OK** to create keystore

### Option C: Command Line (keytool)
```bash
keytool -genkey -v -keystore ffinder-release.jks -alias ffinder-release -keyalg RSA -keysize 2048 -validity 10000
```

## Step 2: Configure Gradle Properties

Create or edit `gradle.properties` file in your project root and add:

```properties
# Release signing config
RELEASE_STORE_FILE=../ffinder-release.jks
RELEASE_STORE_PASSWORD=your_keystore_password_here
RELEASE_KEY_ALIAS=ffinder-release
RELEASE_KEY_PASSWORD=your_key_password_here

# Maps API Keys - Current API key configured for both debug and release
MAPS_API_KEY_DEBUG=AIzaSyDynUiAXTN354tCutKG7dDc2iyOROLKuqQ
MAPS_API_KEY_RELEASE=AIzaSyDynUiAXTN354tCutKG7dDc2iyOROLKuqQ
```

**Important**: 
- Never commit `gradle.properties` with real credentials to version control
- Use relative path for keystore file or absolute path
- Keep keystore file secure and backed up
- **Maps API key is already configured** - you can use the existing key initially

## Step 3: Get Release SHA-1 Fingerprint

Run the signing report command:

```bash
./gradlew signingReport
```

Look for the **release** variant output and copy the **SHA1** fingerprint:
```
Variant: release
Config: release
Store: /path/to/ffinder-release.jks
Alias: ffinder-release
MD5: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
SHA-256: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
Valid until: [date]
```

## Step 4: Configure Firebase for Release

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your FFinder project
3. Go to **Project Settings** → **General** tab
4. Under **Your apps**, find your Android app or add a new one:
   - **Package name**: `com.locationsharing.app` (no .debug suffix)
   - **App nickname**: `FFinder Release`
5. Add the **SHA-1 fingerprint** from Step 3 to the app
6. Download the updated `google-services.json` file
7. Replace `android/app/google-services.json` with the new file

## Step 5: Configure Google Maps API for Release

**Current Status**: The Maps API key `AIzaSyDynUiAXTN354tCutKG7dDc2iyOROLKuqQ` is already configured.

**To secure it for production:**

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to **APIs & Services** → **Credentials**
3. Find the API key `AIzaSyDynUiAXTN354tCutKG7dDc2iyOROLKuqQ`
4. Click on the API key to edit restrictions
5. Under **Application restrictions**:
   - Select **Android apps**
   - Click **Add package name and fingerprint**
   - **Package name**: `com.locationsharing.app`
   - **SHA-1 certificate fingerprint**: (from Step 3)
   - **Package name**: `com.locationsharing.app.debug` (for debug builds)
   - **SHA-1 certificate fingerprint**: (debug SHA-1 from Step 3)
6. Save the changes

**Note**: The API key is already in `gradle.properties` - no need to copy it again.

## Step 6: Build Release APK

Run the release build command:

```bash
./gradlew :app:assembleRelease
```

### APK Location
The signed release APK will be generated at:
```
android/app/build/outputs/apk/release/app-release.apk
```

### Build Verification
Check the build output for:
- ✅ **BUILD SUCCESSFUL**
- ✅ No signing errors
- ✅ APK size reasonable (typically 20-50MB for FFinder)

## Step 7: Install and Test

### Install via ADB
```bash
adb install -r android/app/build/outputs/apk/release/app-release.apk
```

### Side-loading Instructions for Testers

**For testers receiving the APK:**

1. **Enable Unknown Sources** (Android 8.0+):
   - Go to **Settings** → **Security & privacy** → **More security settings**
   - Enable **Install apps from external sources**
   - Or when installing, Android will prompt to enable for your browser/file manager

2. **Install the APK**:
   - Download `app-release.apk` to your device
   - Tap on the file in Downloads or file manager
   - Tap **Install** when prompted
   - Grant permissions as needed

3. **Launch and Test**:
   - Open **FFinder** from app drawer
   - Test core features:
     - Location permissions
     - Map loading
     - Anonymous authentication
     - Location sharing toggle
     - Contact discovery (if enabled)

## Release Checklist

Before distributing:
- [ ] Release APK built successfully
- [ ] Installed and launched on test device
- [ ] Firebase authentication working
- [ ] Google Maps loading correctly
- [ ] Location permissions requested
- [ ] All core features functional
- [ ] No debug logs visible
- [ ] App icon and name correct
- [ ] Package ID is `com.locationsharing.app` (not `.debug`)

## Security Notes

- **Keep keystore secure**: The keystore file and passwords are critical for future releases
- **Backup keystore**: Store keystore in a secure, backed-up location
- **Never commit secrets**: Don't commit `gradle.properties` with real credentials
- **API key restrictions**: Ensure Maps API key is restricted to your release package/fingerprint

## Troubleshooting

### Build Failures
- **Keystore not found**: Check `RELEASE_STORE_FILE` path in `gradle.properties`
- **Wrong password**: Verify `RELEASE_STORE_PASSWORD` and `RELEASE_KEY_PASSWORD`
- **Key alias not found**: Check `RELEASE_KEY_ALIAS` matches keystore

### Runtime Issues
- **Maps not loading**: Verify Maps API key and restrictions in Google Cloud Console
- **Firebase errors**: Ensure `google-services.json` has correct package name and SHA-1
- **Install blocked**: Enable "Install from unknown sources" on device

### Performance Issues
- If app is slow, consider enabling minification for future releases
- Current release has `minifyEnabled = false` to avoid R8 issues initially

## Next Steps for Future Releases

1. **Google Play Console**: Set up app in Play Console for store distribution
2. **Enable Minification**: After testing, enable `minifyEnabled = true` and `shrinkResources = true`
3. **App Bundles**: Consider using AAB format for Play Store: `./gradlew :app:bundleRelease`
4. **Automated Signing**: Set up CI/CD with secure keystore management
5. **Version Management**: Increment `versionCode` and `versionName` for each release