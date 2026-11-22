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
  com.huangder.lumibooks.ui.reader.engine.ReaderVariableFontsInstrumentedTest
)
minimum_tests=21
if [[ "${LUMI_DEVICE_EXTENDED:-0}" == 1 ]]; then
  classes+=(
    com.huangder.lumibooks.data.local.ReaderPreferencesSnapshotTest
    com.huangder.lumibooks.ui.welcome.WelcomeScreenInstrumentedTest
  )
  minimum_tests=25
fi
IFS=,
test_classes="${classes[*]}"
unset IFS
echo "Instrumented smoke classes: $test_classes" | tee test-logs/device/classes.txt
# Gradle is authoritative for artifact availability; no speculative HTTP probes.
set +e
timeout --signal=TERM --kill-after=30s 46m \
  ./gradlew --no-daemon --console=plain --stacktrace --max-workers=2 \
    :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=$test_classes" \
    2>&1 | tee test-logs/device/gradle.log
rc=$?
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
  for png in icon-style-before.png icon-style-after.png; do
    if [[ ! -s "test-logs/device/screenshots/$png" ]]; then
      echo "::error::Required screenshot $png was not exported from API $sdk MediaStore" >&2
      exit 1
    fi
  done
fi
exit "$report_rc"
