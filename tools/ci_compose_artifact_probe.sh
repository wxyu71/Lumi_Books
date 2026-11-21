#!/usr/bin/env bash
# Distinguish CDN/repository misses from Gradle errors on isolated emulator hosts.
# This probe is informational. Instrumentation + per-class JUnit remain required.
set -euo pipefail
mkdir -p test-logs/device
log=test-logs/device/compose-repositories.txt
: > "$log"
modules=(
  org/jetbrains/compose/runtime/runtime-saveable/1.7.1/runtime-saveable-1.7.1.pom
  org/jetbrains/compose/ui/ui-graphics/1.7.1/ui-graphics-1.7.1.pom
)
repositories=(
  https://repo1.maven.org/maven2
  https://maven-central.storage-download.googleapis.com/maven2
  https://repo.maven.apache.org/maven2
)
for pom in "${modules[@]}"; do
  hit=0
  for repository in "${repositories[@]}"; do
    status="$(curl --location --silent --show-error --connect-timeout 5 --max-time 12 \
      --output /dev/null --write-out '%{http_code}' "$repository/$pom" 2>> "$log")" || status="unreachable($status)"
    printf "JetBrains Compose 1.7.1 POM | %s | %s | HTTP %s\n" "$pom" "$repository" "$status" | tee -a "$log"
    if [[ "$status" == 200 ]]; then hit=1; fi
  done
  if (( hit == 0 )); then
    echo "::warning::No checked Maven Central endpoint returned HTTP 200 for $pom; Gradle/JUnit is the authoritative result"
  fi
done
{
  echo "### JetBrains Compose 1.7.1 Maven GET probe"
  cat "$log"
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
