#!/usr/bin/env bash
# Ricompila l'APK release (SDK in local.properties, Gradle 8.14.3 dalla cache del wrapper)
set -e
cd "$(dirname "$0")"
GRADLE=$(ls -d ~/.gradle/wrapper/dists/gradle-8.14.3-bin/*/gradle-8.14.3 | head -1)/bin/gradle
"$GRADLE" assembleRelease
VERSION=$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)
mkdir -p dist
cp app/build/outputs/apk/release/app-release.apk "dist/MyExperience-v$VERSION.apk"
echo "APK: $(pwd)/dist/MyExperience-v$VERSION.apk"
