#!/usr/bin/env python3
"""Fail closed if a Gradle connectedAndroidTest run has no real XML results."""
from pathlib import Path
import os
import sys
import xml.etree.ElementTree as ET

def inspect_reports(directory: Path, minimum: int, required_classes: tuple[str, ...] = ()) -> tuple[int, int, int, list[str]]:
    paths = sorted(directory.rglob("*.xml")) if directory.exists() else []
    # Android Gradle Test results contain TEST-*.xml. Never count unrelated files.
    paths = [p for p in paths if p.name.startswith("TEST-")]
    cases = failures = skipped = 0
    observed_classes = set()
    errors = []
    for path in paths:
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as exc:
            errors.append(f"{path}: invalid XML: {exc}")
            continue
        for item in root.iter("testcase"):
            observed_classes.add(item.get("classname", ""))
            cases += 1
            for child in item:
                if child.tag in {"failure", "error"}:
                    failures += 1
                    errors.append(f"{item.get('classname', '')}.{item.get('name', '')}: {child.get('message', '')}")
                elif child.tag == "skipped":
                    skipped += 1
    if cases < minimum:
        errors.append(f"Android JUnit inventory too small: executed={cases}, minimum={minimum}, XML files={len(paths)}")
    for expected in required_classes:
        if expected not in observed_classes:
            errors.append(f"Required instrumentation class did not execute: {expected}")
    if failures:
        errors.append(f"Android JUnit failures/errors: {failures}")
    return cases, failures, skipped, errors

def main() -> int:
    if len(sys.argv) != 4:
        print("usage: ci_verify_android_junit.py <results-dir> <minimum-tests> <comma-separated-required-classes>", file=sys.stderr)
        return 2
    directory, minimum = Path(sys.argv[1]), int(sys.argv[2])
    required = tuple(v.strip() for v in sys.argv[3].split(",") if v.strip())
    cases, failures, skipped, errors = inspect_reports(directory, minimum, required)
    summary = f"Android instrumented JUnit: {cases} executed, {failures} failures, {skipped} skipped, minimum {minimum}"
    print(summary)
    for error in errors[:25]:
        print(f"::error::{error}", file=sys.stderr)
    report = os.environ.get("GITHUB_STEP_SUMMARY")
    if report:
        with open(report, "a", encoding="utf-8") as output:
            output.write(f"\n### Emulator instrumented verification\n{summary}\n")
            for error in errors[:15]:
                output.write(f"- {error}\n")
    return 1 if errors else 0

if __name__ == "__main__":
    sys.exit(main())
