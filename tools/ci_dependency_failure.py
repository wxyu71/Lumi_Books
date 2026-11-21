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
    # A missing plugin-marker message alone is not a proven transient outage.
    # CI #81 retried the identical unresolved marker three times to no avail.
    # Only explicit network failures qualify for a bounded retry.
    return bool(NETWORK_MARKERS.search(log))

def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: ci_dependency_failure.py <Gradle-attempt-log>", file=sys.stderr)
        return 2
    return 0 if is_retryable_resolution_failure(
        Path(sys.argv[1]).read_text("utf-8", errors="replace")
    ) else 1

if __name__ == "__main__":
    raise SystemExit(main())
