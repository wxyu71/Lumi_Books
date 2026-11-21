#!/usr/bin/env python3
"""Require the complete Android Lint XML report to contain zero errors.

This verifier does not allowlist existing issues, disable checks or consult a
baseline. It also fails closed on a missing, malformed or empty report.
"""
import os
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def verify(report: Path) -> tuple[int, int, int, list[str]]:
    root = ET.parse(report).getroot()
    if root.tag != "issues":
        raise ValueError("Expected Android Lint <issues> XML")
    issues = list(root.iter("issue"))
    errors, warnings, hints = [], 0, 0
    for issue in issues:
        severity = issue.get("severity", "")
        if severity in ("Error", "Fatal"):
            location = issue.find("location")
            where = ""
            if location is not None:
                where = f" ({location.get('file', '?')}:{location.get('line', '?')})"
            errors.append(f"{issue.get('id', 'Unknown')}: {issue.get('message', '')}{where}")
        elif severity == "Warning":
            warnings += 1
        else:
            hints += 1
    # A valid zero-finding report is possible, but only if Lint supplied format
    # and byproduct metadata identifying it as a real completed Lint report.
    if not root.attrib:
        raise ValueError("Unidentifiable Lint report: missing report metadata")
    return len(errors), warnings, hints, errors


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: ci_require_clean_lint.py <lint-results.xml>", file=sys.stderr)
        return 2
    try:
        error_count, warning_count, hint_count, details = verify(Path(sys.argv[1]))
    except (ET.ParseError, OSError, ValueError) as exc:
        print(f"::error::Invalid or missing full Android Lint XML: {exc}", file=sys.stderr)
        return 1
    summary = (f"Full Android Lint: {error_count} errors, "
               f"{warning_count} warnings, {hint_count} other findings")
    print(summary)
    for issue in details[:40]:
        print(f"::error::{issue}", file=sys.stderr)
    step = os.environ.get("GITHUB_STEP_SUMMARY")
    if step:
        with open(step, "a", encoding="utf-8") as out:
            out.write(f"\n### Android Lint (zero-error gate)\n{summary}\n")
            for issue in details[:20]:
                out.write(f"- {issue}\n")
    return 1 if error_count else 0


if __name__ == "__main__":
    sys.exit(main())
