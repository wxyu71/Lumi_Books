#!/usr/bin/env python3
"""Only classify Gradle dependency resolution errors for bounded CI retries."""
from pathlib import Path
import re
import sys

NETWORK_MARKERS = re.compile(
    r"(Could not (?:GET|HEAD|resolve host)|Read timed out|Connection reset|"
    r"Temporary failure in name resolution|Remote host terminated the handshake|"
    r"status code (?:429|5\d\d)|(?:HTTP|response status) (?:429|5\d\d))",
    re.IGNORECASE,
)

def is_retryable_resolution_failure(log: str) -> bool:
    """Never rerun compile/test errors to manufacture a green build."""
    if re.search(
        r"(?:e:\s+file:/|Compilation error\.|There were failing tests|"
        r"\d+\s+tests completed,?\s+\d+\s+failed|AssertionError|Test failed|BUILD SUCCESSFUL)",
        log, re.IGNORECASE,
    ):
        return False
    # Hilt 2.58's implementation AND marker have verifiable Maven Central
    # coordinates. This precise "not found" failure is allowed a limited
    # refresh-dependencies retry because Gradle caches failed lookups.
    # Never generalize this exception to arbitrary versions/plugins.
    published_hilt = (
        "Plugin [id: 'com.google.dagger.hilt.android', version: '2.58'" in log
        and "not found in any of the following sources" in log
        and (
            "com.google.dagger:hilt-android-gradle-plugin:2.58" in log
            or "com.google.dagger.hilt.android.gradle.plugin:2.58" in log
        )
    )
    # CI #90: an isolated API 29 runner saw multiple 404-like misses for
    # existing JetBrains Compose 1.7.1 modules required by Haze. The same
    # pinned test sources passed on API 35 and earlier both API levels.
    # Permit a bounded metadata refresh ONLY for these exact coordinates
    # and only within Android instrumented dependency resolution.
    compose_androidtest = (
        "debugAndroidTestCompileClasspath" in log
        and "Searched in the following locations:" in log
        and bool(re.search(
            r"Could not find org[.]jetbrains[.]compose[.](?:runtime|ui):"
            r"(?:runtime-saveable|ui-geometry|ui-graphics|ui-unit):1[.]7[.]1[.]",
            log,
        ))
    )
    return published_hilt or compose_androidtest or bool(NETWORK_MARKERS.search(log))

def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: ci_dependency_failure.py <Gradle-attempt-log>", file=sys.stderr)
        return 2
    return 0 if is_retryable_resolution_failure(
        Path(sys.argv[1]).read_text("utf-8", errors="replace")
    ) else 1

if __name__ == "__main__":
    raise SystemExit(main())
