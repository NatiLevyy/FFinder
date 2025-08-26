# FFinder Release APK - Quick Setup Summary

## ✅ What's Already Configured

1. **Google Maps API Key**: `AIzaSyDynUiAXTN354tCutKG7dDc2iyOROLKuqQ`
   - Added to `gradle.properties` for both debug and release
   - Manifest configured to use build-time placeholders
   - Removed hardcoded key from `strings.xml`

2. **Build Configuration**:
   - Debug package: `com.locationsharing.app.debug`
   - Release package: `com.locationsharing.app`
   - Universal APK (no ABI splits)
   - Minification disabled for first release
   - Proper signing configuration setup

3. **Debug SHA-1 Fingerprint**: `68:D2:D2:EB:D5:0E:37:E4:43:60:EA:CA:C7:70:4C:A4:AB:88:85:DF`

## 🔄 Next Steps to Complete Release

1. **Create Release Keystore** (choose one method):
   - Run `create-release-keystore.bat` (Windows)
   - Run `./create-release-keystore.sh` (Mac/Linux)
   - Use Android Studio UI
   - Use keytool command line

2. **Update gradle.properties** with your keystore credentials:
   ```
   RELEASE_STORE_PASSWORD=your_actual_password
   RELEASE_KEY_PASSWORD=your_actual_password
   ```

3. **Get Release SHA-1**:
   ```bash
   ./gradlew signingReport
   ```

4. **Configure Firebase & Google Cloud**:
   - Add release SHA-1 to Firebase (package: `com.locationsharing.app`)
   - Add release SHA-1 to Google Maps API restrictions

5. **Build Release APK**:
   ```bash
   ./gradlew :app:assembleRelease
   ```
   - APK location: `android/app/build/outputs/apk/release/app-release.apk`

## 🚀 Ready for Testing

Once keystore is created, the app is ready for:
- Side-loading to friends/testers
- Internal testing
- Google Play Console upload (when ready)

## 📱 Package Information

- **Debug**: `com.locationsharing.app.debug` (for development)
- **Release**: `com.locationsharing.app` (for distribution)
- **Maps API**: Already configured and working
- **Firebase**: Will work once release SHA-1 is added

See `README-RELEASE.md` for detailed step-by-step instructions.