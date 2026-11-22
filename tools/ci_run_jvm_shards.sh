#!/usr/bin/env bash
# Execute the same compiled JVM suite in three disjoint Gradle test filters.
# Compilation and Robolectric SDK availability are validated before this step.
set -uo pipefail
failed=0
summary="${GITHUB_STEP_SUMMARY:-/dev/null}"
mkdir -p test-logs
for shard in reader ui core; do
  output="test-logs/$shard"
  mkdir -p "$output"
  rm -rf app/build/oss/test-results/testDebugUnitTest app/build/oss/reports/tests/testDebugUnitTest
  echo "==== Running JVM $shard shard ====" | tee -a "$summary"
  if timeout --signal=TERM --kill-after=30s 22m \
    ./gradlew --no-daemon --console=plain --stacktrace --max-workers=2 \
      "-Plumi.testShard=$shard" :app:testDebugUnitTest 2>&1 | tee "$output/gradle.log"; then
    rc=0
  else
    rc=1
    failed=1
  fi
  results="app/build/oss/test-results/testDebugUnitTest"
  report="app/build/oss/reports/tests/testDebugUnitTest"
  if [[ -d "$results" ]]; then cp -a "$results" "$output/junit"; fi
  if [[ -d "$report" ]]; then cp -a "$report" "$output/html"; fi
  if ! python3 tools/ci_verify_junit.py "$shard" "$output/junit" | tee -a "$summary"; then failed=1; fi
  if (( rc != 0 )); then
    echo "::error::JVM $shard failed (Gradle/test exit $rc)"
    printf '\nJVM %s failed\n' "$shard" >> "$summary"
  fi
  if ! compgen -G "$output/junit/TEST-*.xml" > /dev/null; then
    echo "::error::No JUnit XML for $shard; later shards cannot mask compile/runtime failures"
    exit 1
  fi
done
exit "$failed"
