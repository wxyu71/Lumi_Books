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
  com.huangder.lumibooks.ui.bookshelf.BookshelfTagJourneyInstrumentedTest
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

# Preserve the entire test window, including native crashes before instrumentation
# reports an assertion. Do not clear shared log buffers or treat unrelated OS logs
# as application test failures.
{
  printf 'commit=%s\n' "$(git rev-parse HEAD)"
  adb shell getprop ro.build.version.sdk
  adb shell getprop ro.build.fingerprint
  adb shell wm size
  adb shell wm density
  adb shell settings get system font_scale
  adb shell getprop persist.sys.locale
} > test-logs/device/environment.txt
adb logcat -b all -v threadtime -T 1 > test-logs/device/logcat-full.txt 2>&1 &
log_pid=$!
stop_logcat() {
  kill "$log_pid" 2>/dev/null || true
  wait "$log_pid" 2>/dev/null || true
}
trap stop_logcat EXIT

args=("-Pandroid.testInstrumentationRunnerArguments.class=$test_classes")
if [[ "${LUMI_DEVICE_FULL:-0}" == "1" ]]; then
  args=()
  printf '%s\n' 'ALL (AndroidJUnitRunner discovery)' > test-logs/device/classes.txt
fi
if [[ "${LUMI_DEVICE_EXTENDED:-0}" == "1" ]]; then
  args+=("-Pandroid.testInstrumentationRunnerArguments.lumiExtended=true")
fi

set +e
timeout --signal=TERM --kill-after=30s 46m \
  ./gradlew --no-daemon --console=plain --stacktrace --max-workers=2 \
    :app:connectedDebugAndroidTest \
    "${args[@]}" \
    2>&1 | tee test-logs/device/gradle.log
rc=${PIPESTATUS[0]}
set -e

# Preserve emulator evidence even when instrumentation fails.
adb logcat -d -b crash -v threadtime > test-logs/device/crash.txt 2>&1 || true
adb logcat -d -t 3000 > test-logs/device/logcat.txt 2>&1 || true
adb exec-out screencap -p > test-logs/device/final-screen.png || true
adb shell uiautomator dump /sdcard/lumi-ci-final.xml > test-logs/device/hierarchy.log 2>&1 &&
  adb pull /sdcard/lumi-ci-final.xml test-logs/device/final-hierarchy.xml >> test-logs/device/hierarchy.log 2>&1 || true
stop_logcat
trap - EXIT
adb pull /sdcard/Pictures/LumiCI test-logs/device/screenshots \
  > test-logs/device/screenshot-pull.log 2>&1 || true

if (( rc != 0 )); then
  echo "::error::Android instrumentation exited $rc; see native Gradle/JUnit reports and logcat" >&2
  exit "$rc"
fi

# The screenshot test is only useful if its real output leaves the emulator.
sdk="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
if [[ "$sdk" =~ ^[0-9]+$ ]] && (( sdk >= 29 )); then
  for png in icon-style-before.png icon-style-after.png tag-journey-picker.png tag-journey-manager.png tag-journey-menu.png; do
    test -s "test-logs/device/screenshots/$png" || {
      echo "::error::Required screenshot $png was not exported from API $sdk" >&2
      exit 1
    }
  done
fi
