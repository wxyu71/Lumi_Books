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
for attempt in 1 2 3; do
  attempt_log="$log.attempt$attempt.log"
  echo "Gradle attempt $attempt/3: $*" | tee -a "$log"
  set +e
  timeout --signal=TERM --kill-after=30s 21m ./gradlew --no-daemon --console=plain --stacktrace "$@" \
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
    echo "::warning::Gradle plugin/dependency resolution failed; retrying attempt $attempt/2" | tee -a "$log"
    sleep "$((attempt * 5))"
  else
    echo "Gradle exited $gradle_rc; no retry for compiler/test/build failures" | tee -a "$log"
    exit "$gradle_rc"
  fi
done
exit 1
