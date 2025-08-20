@echo off
echo FFinder Release Keystore Generator
echo ====================================
echo.
echo This script will create a release keystore for signing FFinder APKs.
echo Make sure to remember the passwords you set!
echo.
pause

set /p KEYSTORE_NAME="Enter keystore filename (default: ffinder-release.jks): "
if "%KEYSTORE_NAME%"=="" set KEYSTORE_NAME=ffinder-release.jks

set /p KEY_ALIAS="Enter key alias (default: ffinder-release): "
if "%KEY_ALIAS%"=="" set KEY_ALIAS=ffinder-release

echo.
echo Creating keystore: %KEYSTORE_NAME%
echo Key alias: %KEY_ALIAS%
echo.
echo You will be prompted for:
echo - Keystore password
echo - Key password
echo - Certificate information (name, organization, etc.)
echo.
pause

keytool -genkey -v -keystore %KEYSTORE_NAME% -alias %KEY_ALIAS% -keyalg RSA -keysize 2048 -validity 10000

if %ERRORLEVEL% equ 0 (
    echo.
    echo SUCCESS: Keystore created successfully!
    echo.
    echo Next steps:
    echo 1. Update gradle.properties with your keystore path and passwords
    echo 2. Run: gradlew signingReport
    echo 3. Copy the release SHA-1 to Firebase and Google Cloud Console
    echo 4. Build release: gradlew :app:assembleRelease
    echo.
    echo IMPORTANT: Keep this keystore file safe and backed up!
) else (
    echo.
    echo ERROR: Failed to create keystore
)

pause