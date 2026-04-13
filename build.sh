#!/bin/bash
# ─────────────────────────────────────────────────────────
#  Gym Manager Pro — Build Helper Script
#  Run from the GymManagerPro root directory
# ─────────────────────────────────────────────────────────

set -e

echo ""
echo "🏋️  Gym Manager Pro - Build Script"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check Android SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "⚠️  ANDROID_HOME not set. Please set it to your Android SDK path."
    echo "   Export it: export ANDROID_HOME=/Users/YOU/Library/Android/sdk"
    exit 1
fi

echo "✅ Android SDK found"

# Parse argument
ACTION=${1:-"debug"}

case "$ACTION" in
  "debug")
    echo "📦 Building DEBUG APK..."
    ./gradlew assembleDebug
    echo ""
    echo "✅ Debug APK built!"
    echo "📍 Location: app/build/outputs/apk/debug/app-debug.apk"
    ;;

  "release")
    if [ ! -f "gym_manager.keystore" ]; then
        echo "🔑 No keystore found. Generating one..."
        echo ""
        echo "Enter keystore details:"
        read -p "  Store password: " STORE_PASS
        read -p "  Key password: " KEY_PASS
        read -p "  Your name: " YOUR_NAME
        read -p "  Organization: " ORG
        read -p "  City: " CITY
        read -p "  Country code (e.g. PK): " COUNTRY

        keytool -genkey -v \
          -keystore gym_manager.keystore \
          -alias gym_manager \
          -keyalg RSA -keysize 2048 -validity 10000 \
          -dname "CN=${YOUR_NAME}, OU=${ORG}, O=${ORG}, L=${CITY}, C=${COUNTRY}" \
          -storepass "${STORE_PASS}" \
          -keypass "${KEY_PASS}"

        echo ""
        echo "✅ Keystore created: gym_manager.keystore"
        echo "⚠️  Add to app/build.gradle signingConfigs.release:"
        echo "    storePassword \"${STORE_PASS}\""
        echo "    keyPassword  \"${KEY_PASS}\""
        echo ""
    fi

    echo "📦 Building RELEASE APK..."
    ./gradlew assembleRelease
    echo ""
    echo "✅ Release APK built!"
    echo "📍 Location: app/build/outputs/apk/release/app-release.apk"
    ;;

  "install")
    echo "📲 Installing to connected device..."
    ./gradlew installDebug
    echo "✅ Installed! Open Gym Manager Pro on your device."
    ;;

  "clean")
    echo "🧹 Cleaning build..."
    ./gradlew clean
    echo "✅ Clean done."
    ;;

  *)
    echo "Usage: ./build.sh [debug|release|install|clean]"
    echo ""
    echo "  debug   - Build debug APK (default)"
    echo "  release - Build signed release APK"
    echo "  install - Build and install on connected device"
    echo "  clean   - Clean build files"
    ;;
esac

echo ""
