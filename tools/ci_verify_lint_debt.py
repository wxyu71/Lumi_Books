#!/usr/bin/env python3
"""Fail closed on new Android Lint errors while tracking exact legacy debt.

This is an immutable, location-aware CI snapshot of the 556 errors found when
full lintDebug was first made mandatory. It never disables lint analysis:
new, moved, or changed errors fail the Required Gate with their issue IDs.
"""
from collections import Counter
from pathlib import Path
import hashlib
import os
import sys
import xml.etree.ElementTree as ET

RECORD_BYTES = 8
EXPECTED_LEGACY_RECORDS = 559
EXPECTED_LEGACY_SHA256 = "5ac74c513873117dbd3af1551e93146d4a3a528d88da7a7b61717ecf6a47b542"


def fingerprint(issue: ET.Element, location: ET.Element) -> bytes:
    raw_path = location.get("file", "").replace("\\", "/")
    marker = "/Lumi_Books/Lumi_Books/app/"
    if marker in raw_path:
        raw_path = raw_path.split(marker, 1)[1]
    fields = (issue.get("id", ""), issue.get("message", ""), raw_path,
              location.get("line", ""), location.get("column", ""),
              issue.get("errorLine1", ""))
    return hashlib.sha256("\0".join(fields).encode("utf-8")).digest()[:RECORD_BYTES]


def read_legacy_snapshot(path: Path) -> Counter[bytes]:
    data = path.read_bytes()
    if len(data) != EXPECTED_LEGACY_RECORDS * RECORD_BYTES:
        raise ValueError(f"Unexpected legacy lint snapshot size: {len(data)}")
    actual = hashlib.sha256(data).hexdigest()
    if actual != EXPECTED_LEGACY_SHA256:
        raise ValueError("Lint legacy allowlist was edited; review every change to the baseline")
    return Counter(data[i:i + RECORD_BYTES] for i in range(0, len(data), RECORD_BYTES))


def verify(xml_path: Path, baseline: Counter[bytes]) -> tuple[int, int, list[str]]:
    root = ET.parse(xml_path).getroot()
    if root.tag != "issues":
        raise ValueError("Expected Android Lint <issues> XML, not an unrelated report")
    observed = Counter()
    new_issues = []
    issue_count = 0
    for issue in root.iter("issue"):
        if issue.get("severity") not in ("Error", "Fatal"):
            continue
        issue_count += 1
        locations = issue.findall("location")
        if not locations:
            new_issues.append(f"{issue.get('id')}: no location (cannot be baselined)")
            continue
        for location in locations:
            key = fingerprint(issue, location)
            observed[key] += 1
            if observed[key] > baseline[key]:
                new_issues.append(f"{issue.get('id')}: {issue.get('message')} "
                                  f"({location.get('file')}:{location.get('line')})")
    return issue_count, sum(observed.values()), new_issues


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: ci_verify_lint_debt.py <lint-results.xml> <immutable-fingerprints.bin>", file=sys.stderr)
        return 2
    try:
        legacy = read_legacy_snapshot(Path(sys.argv[2]))
        errors, locations, new = verify(Path(sys.argv[1]), legacy)
    except (OSError, ET.ParseError, ValueError) as error:
        print(f"::error::Lint report or legacy snapshot invalid: {error}", file=sys.stderr)
        return 1
    summary = (f"Full Android Lint: {errors} errors ({locations} locations), "
               f"{len(new)} new/unbaselined error locations; "
               f"{EXPECTED_LEGACY_RECORDS} frozen legacy locations")
    print(summary)
    for item in new[:35]:
        print(f"::error::{item}", file=sys.stderr)
    step = os.environ.get("GITHUB_STEP_SUMMARY")
    if step:
        with open(step, "a", encoding="utf8") as stream:
            stream.write(f"\n### Full Android Lint issue budget\n{summary}\n")
            for item in new[:15]:
                stream.write(f"- {item}\n")
    return 1 if new else 0


if __name__ == "__main__":
    sys.exit(main())
