#!/usr/bin/env bash
# Pubblica una nuova versione: build + GitHub Release con l'APK allegato.
# Prima alza versionCode e versionName in app/build.gradle.kts.
# Uso: ./release.sh "note della release"
set -e
cd "$(dirname "$0")"
./build-apk.sh
VERSION=$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)
gh release create "v$VERSION" "dist/MyExperience-v$VERSION.apk" --title "MyExperience v$VERSION" --notes "${1:-MyExperience v$VERSION}"
