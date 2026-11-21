#!/usr/bin/env bash
# Compile shared Kotlin tests once, run three independently reported package shards.
set -uo pipefail
failed=0
summary="$(printenv GITHUB_STEP_SUMMARY || echo /dev/null)"
mkdir -p test-logs
for shard in reader ui core; do
  output="test-logs/$shard"
  mkdir -p "$output"
  rm -rf app/build/oss/test-results/testDebugUnitTest app/build/oss/reports/tests/testDebugUnitTest
  echo "==== Running JVM $shard shard ====" | tee -a "$summary"
  if bash tools/ci_gradle_retry.sh "$output/gradle.log" "-Plumi.testShard=$shard" :app:testDebugUnitTest; then
    rc=0
  else
    rc=$?
    failed=1
  fi
  result_dir="app/build/oss/test-results/testDebugUnitTest"
  report_dir="app/build/oss/reports/tests/testDebugUnitTest"
  if [[ -d "$result_dir" ]]; then cp -a "$result_dir" "$output/junit"; fi
  if [[ -d "$report_dir" ]]; then cp -a "$report_dir" "$output/html"; fi
  python3 tools/ci_junit_summary.py "$shard" "$output/junit" "$output/gradle.log" >> "$summary" || failed=1
  if ! python3 tools/ci_verify_junit.py "$shard" "$output/junit"; then failed=1; fi
  if (( rc != 0 )); then
    echo "::error::JVM $shard failed with exit $rc"
    printf "\nJVM %s failed (exit %s)\n" "$shard" "$rc" >> "$summary"
  fi
  if ! compgen -G "$output/junit/TEST-*.xml" > /dev/null; then
    echo "::error::No JUnit XML for $shard; abort before duplicate compiler failures"
    exit 1
  fi
  if (( rc == 124 || rc == 137 || rc == 143 )); then
    echo "::error::JVM $shard timed out or was interrupted"
    exit 1
  fi
done
exit "$failed"
