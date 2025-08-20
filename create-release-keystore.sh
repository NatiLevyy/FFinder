#!/bin/bash

echo "FFinder Release Keystore Generator"
echo "===================================="
echo ""
echo "This script will create a release keystore for signing FFinder APKs."
echo "Make sure to remember the passwords you set!"
echo ""
read -p "Press Enter to continue..."

read -p "Enter keystore filename (default: ffinder-release.jks): " KEYSTORE_NAME
KEYSTORE_NAME=${KEYSTORE_NAME:-ffinder-release.jks}

read -p "Enter key alias (default: ffinder-release): " KEY_ALIAS
KEY_ALIAS=${KEY_ALIAS:-ffinder-release}

echo ""
echo "Creating keystore: $KEYSTORE_NAME"
echo "Key alias: $KEY_ALIAS"
echo ""
echo "You will be prompted for:"
echo "- Keystore password"
echo "- Key password"
echo "- Certificate information (name, organization, etc.)"
echo ""
read -p "Press Enter to continue..."

keytool -genkey -v -keystore "$KEYSTORE_NAME" -alias "$KEY_ALIAS" -keyalg RSA -keysize 2048 -validity 10000

if [ $? -eq 0 ]; then
    echo ""
    echo "SUCCESS: Keystore created successfully!"
    echo ""
    echo "Next steps:"
    echo "1. Update gradle.properties with your keystore path and passwords"
    echo "2. Run: ./gradlew signingReport"
    echo "3. Copy the release SHA-1 to Firebase and Google Cloud Console"
    echo "4. Build release: ./gradlew :app:assembleRelease"
    echo ""
    echo "IMPORTANT: Keep this keystore file safe and backed up!"
else
    echo ""
    echo "ERROR: Failed to create keystore"
fi

echo ""
read -p "Press Enter to exit..."