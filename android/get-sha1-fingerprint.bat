@echo off
REM Get SHA1 Fingerprint for Google Maps API Key Configuration
REM This script displays the SHA1 fingerprints needed for Google Cloud Console

echo === FFinder Google Maps API Key SHA1 Fingerprints ===
echo.

echo DEBUG BUILD SHA1 FINGERPRINT:
echo Use this for development/testing

set DEBUG_KEYSTORE=%USERPROFILE%\.android\debug.keystore
if exist "%DEBUG_KEYSTORE%" (
    echo.
    keytool -list -v -keystore "%DEBUG_KEYSTORE%" -alias androiddebugkey -storepass android -keypass android | findstr "SHA1:"
    echo.
    echo Google Cloud Console Configuration:
    echo Package Name: com.locationsharing.app
    echo SHA1: [Use the SHA1 value shown above]
    echo.
) else (
    echo Debug keystore not found at: %DEBUG_KEYSTORE%
    echo Run an Android project first to generate the debug keystore
    echo.
)

echo RELEASE BUILD SHA1 FINGERPRINT:
echo Use this for production builds

set RELEASE_KEYSTORE=.\app\release.keystore
if exist "%RELEASE_KEYSTORE%" (
    echo Found release keystore. Please run this manually with your credentials:
    echo keytool -list -v -keystore %RELEASE_KEYSTORE% -alias YOUR_ALIAS
    echo.
) else (
    echo No release keystore found at: %RELEASE_KEYSTORE%
    echo Create a release keystore for production builds
    echo.
)

echo === Next Steps ===
echo 1. Go to Google Cloud Console: https://console.cloud.google.com/apis/credentials
echo 2. Select your API key for: AIzaSyDynUiAXTN354tCutKG7dDc2iyOROLKuqQ
echo 3. Under 'Application restrictions', select 'Android apps'
echo 4. Add the package name and SHA1 fingerprint shown above
echo 5. Save the configuration
echo.
echo API Key: AIzaSyDynUiAXTN354tCutKG7dDc2iyOROLKuqQ

pause