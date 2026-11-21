#!/usr/bin/env bash
# Required on real Android emulators: Room integration, Compose interactions and screenshot diff.
set -uo pipefail
mkdir -p test-logs/device
chmod +x ./gradlew
classes=(
  com.huangder.lumibooks.data.local.dao.FolderDaoTest
  com.huangder.lumibooks.data.local.database.DatabaseMigrationsTest
  com.huangder.lumibooks.ui.components.LiquidGlassButtonInteractionInstrumentedTest
  com.huangder.lumibooks.ui.settings.AppIconStyleScreenInstrumentedTest
  com.huangder.lumibooks.ui.settings.AppIconScreenshotInstrumentedTest
)
minimum_tests=20
if [[ "${LUMI_DEVICE_EXTENDED:-0}" == 1 ]]; then
  classes+=(
    com.huangder.lumibooks.data.local.ReaderPreferencesSnapshotTest
    com.huangder.lumibooks.ui.welcome.WelcomeScreenInstrumentedTest
  )
  minimum_tests=24
fi
IFS=,
test_classes="${classes[*]}"
unset IFS
echo "Instrumented smoke classes: $test_classes" | tee test-logs/device/classes.txt
set +e
timeout --signal=TERM --kill-after=30s 40m ./gradlew --no-daemon --console=plain --stacktrace \
  :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=$test_classes" \
  2>&1 | tee test-logs/device/gradle.log
rc=${PIPESTATUS[0]}
set -e
# Always capture independent screenshots and emulator logs on assertion failures.
adb logcat -d -t 3000 > test-logs/device/logcat.txt 2>&1 || true
# Screenshots are written via MediaStore to a public test-only folder.
# App-specific external files are deleted when instrumentation uninstalls APKs.
adb pull /sdcard/Pictures/LumiCI \
  test-logs/device/screenshots > test-logs/device/screenshot-pull.log 2>&1 || true
if (( rc != 0 )); then
  echo "::error::Android instrumentation exited $rc; see gradle.log/logcat/JUnit"
fi
python3 tools/ci_verify_android_junit.py app/build/oss/outputs/androidTest-results/connected "$minimum_tests" "$test_classes"
report_rc=$?
if (( rc != 0 )); then exit "$rc"; fi
# A green screenshot test is not enough: the PNG evidence must leave the
# emulator before Android Emulator Runner tears it down.
sdk="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
if [[ "$sdk" =~ ^[0-9]+$ ]] && (( sdk >= 29 )); then
  if [[ ! -d test-logs/device/screenshots ]] ||
     ! find test-logs/device/screenshots -type f -name '*.png' | grep -q .; then
    echo "::error::Screenshot PNGs were not exported from API $sdk MediaStore" >&2
    exit 1
  fi
fi
exit "$report_rc"
