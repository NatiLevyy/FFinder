# Google Maps API Setup Guide

## Root Cause of App Freeze

The app was freezing because the Google Maps API key was not properly configured. The placeholder `YOUR_API_KEY_HERE` was never replaced with a valid API key, causing the Maps SDK to fail initialization and block the main UI thread.

## Setup Instructions

### 1. Get Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - Maps SDK for Android
   - Places API (if using places features)
   - Geocoding API (if using address lookup)

4. Go to **Credentials** → **Create Credentials** → **API Key**
5. Copy the generated API key

### 2. Configure API Key Restrictions (Recommended)

1. Click on your API key in the credentials list
2. Under **Application restrictions**, select **Android apps**
3. Add your app's package name: `com.locationsharing.app`
4. Add your SHA-1 certificate fingerprint:
   ```bash
   # For debug builds
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   
   # For release builds
   keytool -list -v -keystore your-release-key.keystore -alias your-key-alias
   ```

### 3. Update App Configuration

Replace the placeholder in `android/app/src/main/res/values/strings.xml`:

```xml
<string name="google_maps_api_key">YOUR_ACTUAL_API_KEY_HERE</string>
```

### 4. Test the Configuration

1. Clean and rebuild the project:
   ```bash
   cd android
   ./gradlew clean
   ./gradlew build
   ```

2. Run the app and verify the map loads without errors

## Security Best Practices

1. **Never commit API keys to version control**
2. **Use different keys for debug/release builds**
3. **Restrict API key usage to your app only**
4. **Monitor API usage in Google Cloud Console**

## Troubleshooting

### Common Issues:

1. **"API key not found"** - Check that the key is properly set in strings.xml
2. **"This API project is not authorized"** - Verify package name and SHA-1 fingerprint
3. **"Quota exceeded"** - Check your API usage limits in Google Cloud Console

### Debug Commands:

```bash
# Check current SHA-1 fingerprint
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1

# View app logs for Maps errors
adb logcat | grep "Google Maps Android API"
```

## Environment Variables (Alternative)

For CI/CD or team development, you can use environment variables:

1. Create `local.properties` (not committed):
   ```
   GOOGLE_MAPS_API_KEY=your_api_key_here
   ```

2. Update `build.gradle.kts`:
   ```kotlin
   android {
       defaultConfig {
           val localProperties = Properties()
           val localPropertiesFile = rootProject.file("local.properties")
           if (localPropertiesFile.exists()) {
               localProperties.load(FileInputStream(localPropertiesFile))
           }
           
           resValue("string", "google_maps_api_key", 
               localProperties.getProperty("GOOGLE_MAPS_API_KEY", "YOUR_API_KEY_HERE"))
       }
   }
   ```

This setup prevents the app freeze and ensures proper Maps functionality.