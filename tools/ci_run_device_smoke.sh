#!/usr/bin/env bash
# Shared emulator smoke runner. Gradle/AndroidJUnitRunner are authoritative for
# test discovery and failures; this script only selects classes and preserves evidence.
set -euo pipefail

mkdir -p test-logs/device
chmod +x ./gradlew

classes=(
  com.huangder.lumibooks.data.local.dao.FolderDaoTest
  com.huangder.lumibooks.data.local.database.DatabaseMigrationsTest
  com.huangder.lumibooks.ui.components.LiquidGlassButtonInteractionInstrumentedTest
  com.huangder.lumibooks.ui.tags.CommunityTagManagementInstrumentedTest
  com.huangder.lumibooks.ui.settings.AppIconStyleScreenInstrumentedTest
  com.huangder.lumibooks.ui.settings.AppIconScreenshotInstrumentedTest
  com.huangder.lumibooks.ui.reader.engine.ReaderVariableFontsInstrumentedTest
)
if [[ "${LUMI_DEVICE_EXTENDED:-0}" == "1" ]]; then
  classes+=(
    com.huangder.lumibooks.data.local.ReaderPreferencesSnapshotTest
    com.huangder.lumibooks.ui.welcome.WelcomeScreenInstrumentedTest
  )
fi

IFS=,
test_classes="${classes[*]}"
unset IFS
printf '%s\n' "$test_classes" > test-logs/device/classes.txt

set +e
timeout --signal=TERM --kill-after=30s 46m \
  ./gradlew --no-daemon --console=plain --stacktrace --max-workers=2 \
    :app:connectedDebugAndroidTest \
    "-Pandroid.testInstrumentationRunnerArguments.class=$test_classes" \
    2>&1 | tee test-logs/device/gradle.log
rc=${PIPESTATUS[0]}
set -e

# Preserve emulator evidence even when instrumentation fails.
adb logcat -d -t 3000 > test-logs/device/logcat.txt 2>&1 || true
adb pull /sdcard/Pictures/LumiCI test-logs/device/screenshots \
  > test-logs/device/screenshot-pull.log 2>&1 || true

if (( rc != 0 )); then
  echo "::error::Android instrumentation exited $rc; see native Gradle/JUnit reports and logcat" >&2
  exit "$rc"
fi

# The screenshot test is only useful if its real output leaves the emulator.
sdk="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
if [[ "$sdk" =~ ^[0-9]+$ ]] && (( sdk >= 29 )); then
  for png in icon-style-before.png icon-style-after.png; do
    test -s "test-logs/device/screenshots/$png" || {
      echo "::error::Required screenshot $png was not exported from API $sdk" >&2
      exit 1
    }
  done
fi
