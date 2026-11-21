#!/usr/bin/env bash
# Probe the exact Hilt POM that intermittent GitHub-hosted Gradle jobs cannot retrieve.
# This is diagnostic only: no secrets or version changes and no failures masked.
set -euo pipefail
mkdir -p test-logs
report=test-logs/hilt-2.58-repositories.txt
: > "$report"
pom='com/google/dagger/hilt-android-gradle-plugin/2.58/hilt-android-gradle-plugin-2.58.pom'
mirrors=(
  'https://maven-central.storage-download.googleapis.com/maven2'
  'https://repo.maven.apache.org/maven2'
  'https://repo1.maven.org/maven2'
)
found=0
for origin in "${mirrors[@]}"; do
  status="$(curl --location --silent --show-error --connect-timeout 8 --max-time 20 \
    --output /dev/null --write-out '%{http_code}' "$origin/$pom" 2>> "$report")" || status="unreachable($status)"
  printf 'Hilt 2.58 POM | %s | HTTP %s\n' "$origin" "$status" | tee -a "$report"
  if [[ "$status" == 200 ]]; then found=1; fi
done
if (( found == 0 )); then
  echo '::warning::Hilt plugin POM unavailable at tested endpoints; Gradle is authoritative'
fi
{
  echo '### Hilt 2.58 POM HTTP GET probe'
  cat "$report"
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
