#!/usr/bin/env python3
"""Summarize failing/slow JUnit tests in the GitHub Job Summary (no dependencies).

Always informational: the Gradle command and the JUnit verifier remain blocking.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


def safe_text(value: str, limit: int = 240) -> str:
    return " ".join((value or "").split()).replace("|", "\\|")[:limit]


def render(shard: str, results: Path, log: Path) -> str:
    files = sorted(results.glob("TEST-*.xml")) if results.is_dir() else []
    tests = 0
    skipped = 0
    failures = []
    slow = []
    errors = []
    for file in files:
        try:
            root = ET.parse(file).getroot()
        except (ET.ParseError, OSError) as exc:
            errors.append(f"{file.name}: {safe_text(str(exc))}")
            continue
        for case in root.iter("testcase"):
            tests += 1
            name = safe_text(case.get("classname", "") + "." + case.get("name", ""), 180)
            if case.find("skipped") is not None:
                skipped += 1
            try:
                duration = float(case.get("time", "0"))
            except (ValueError, TypeError):
                duration = 0.0
            slow.append((duration, name))
            for tag in ("failure", "error"):
                for child in case.findall(tag):
                    detail = (child.text or "").strip()
                    message = safe_text(child.get("message") or (detail.splitlines()[0] if detail else ""))
                    failures.append((name, message))
    lines = [
        f"### JVM {safe_text(shard)} — results",
        "",
        f"JUnit XML: **{len(files)} files**, **{tests} testcases**, "
        f"**{len(failures)} failures/errors**, **{skipped} skipped**.",
    ]
    if errors:
        lines += ["", "**Unreadable JUnit files:**"] + [f"- {e}" for e in errors[:5]]
    if failures:
        lines += ["", "**Failing testcases (first 12):**"]
        lines.extend(f"- `{name}`: {message}" for name, message in failures[:12])
    if slow:
        lines += ["", "**Slowest tests:**", "| Duration | Test |", "| ---: | --- |"]
        lines.extend(f"| {seconds:.2f} s | `{name}` |"
                     for seconds, name in sorted(slow, reverse=True)[:8])
    if tests == 0:
        lines += ["", "No executable JUnit cases were reported; the task may have "
                 "failed during dependency resolution or test compilation."]
    if log.is_file():
        text = log.read_text("utf-8", errors="replace")
        markers = [safe_text(line, 320) for line in text.splitlines()
                   if re.search(r"(?i)(^|\s)(e: |error: |FAILURE:|> Task .*FAILED|Execution failed|Unresolved reference)", line)]
        if markers:
            lines += ["", "**Last build/compiler diagnostics:**"]
            lines += [f"- `{line}`" for line in markers[-8:]]
    return "\n".join(lines) + "\n"


def main() -> int:
    if len(sys.argv) != 4:
        print("Usage: ci_junit_summary.py <shard> <XML-directory> <build-log>", file=sys.stderr)
        return 2
    print(render(sys.argv[1], Path(sys.argv[2]), Path(sys.argv[3])))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
