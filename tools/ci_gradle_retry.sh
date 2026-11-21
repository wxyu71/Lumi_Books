#!/usr/bin/env bash
# Retry only identified Gradle dependency resolution errors (never test/compiler errors).
set -euo pipefail
if (( $# < 2 )); then
  echo "Usage: $0 <aggregate-log> <Gradle arguments...>" >&2
  exit 2
fi
log="$1"
shift
mkdir -p "$(dirname "$log")"
: > "$log"
# Failed resolution metadata may be cached even when Maven Central recovers.
# A retry MUST ask Gradle to refresh that negative cache; repeating the same
# command simply fails against the same locally cached "not found" result.
retry_flags=()
for attempt in 1 2 3; do
  attempt_log="$log.attempt$attempt.log"
  echo "Gradle attempt $attempt/3: ${retry_flags[*]} $*" | tee -a "$log"
  set +e
  timeout --signal=TERM --kill-after=30s 21m ./gradlew --no-daemon --console=plain --stacktrace "${retry_flags[@]}" "$@" \
    2>&1 | tee "$attempt_log"
  gradle_rc=$?
  set -e
  cat "$attempt_log" >> "$log"
  if (( gradle_rc == 0 )); then exit 0; fi
  if (( gradle_rc == 124 || gradle_rc == 137 || gradle_rc == 143 )); then
    echo "Gradle exited $gradle_rc; no retry for timeout or interruption" | tee -a "$log"
    exit "$gradle_rc"
  fi
  if (( attempt < 3 )) && python3 tools/ci_dependency_failure.py "$attempt_log"; then
    echo "::warning::Verified published plugin or transient network resolution failed; retrying with --refresh-dependencies (retry $attempt/2)" | tee -a "$log"
    retry_flags=(--refresh-dependencies --info)
    sleep "$((attempt * 15))"
  else
    echo "Gradle exited $gradle_rc; no retry for compiler/test/build failures" | tee -a "$log"
    exit "$gradle_rc"
  fi
done
exit 1
