#!/usr/bin/env bash

set -euo pipefail

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
SCREENSHOT_DIR="artifacts/screenshots"
PACKAGE_NAME="com.aakash.astro"

if [[ ! -f "$APK_PATH" ]]; then
  echo "Debug APK not found at $APK_PATH. Did you run ./gradlew assembleDebug?" >&2
  exit 1
fi

mkdir -p "$SCREENSHOT_DIR"

adb wait-for-device

# Keep the emulator awake/unlocked so screenshots show the UI.
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell wm dismiss-keyguard || true

adb install -r "$APK_PATH"

capture_screen() {
  local output_name=$1
  local activity=$2
  shift 2 || true

  adb shell am force-stop "$PACKAGE_NAME" || true
  adb shell am start -W -n "$PACKAGE_NAME/$activity" "$@" >/dev/null

  # Give the activity a moment to finish rendering.
  sleep 8

  adb exec-out screencap -p > "$SCREENSHOT_DIR/$output_name"
  echo "Captured $output_name"
}

capture_screen "main_activity.png" ".MainActivity"
capture_screen "privacy_policy.png" ".PrivacyActivity"
capture_screen "saved_horoscopes.png" ".SavedHoroscopesActivity"

echo "All screenshots saved to $SCREENSHOT_DIR"
