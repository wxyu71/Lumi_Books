#!/usr/bin/env python3
"""Reject empty or misrouted JUnit runs and expose per-shard test totals."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

if len(sys.argv) != 3 or sys.argv[1] not in ("reader", "ui", "core"):
    raise SystemExit("Usage: ci_verify_junit.py <reader|ui|core> <result-directory>")
shard = sys.argv[1]
folder = Path(sys.argv[2])
files = sorted(folder.glob("TEST-*.xml"))
if not files:
    raise SystemExit(f"No JUnit XML reports found for {shard}")
prefix = "com.huangder.lumibooks.ui."
count = 0
failures = 0
for file in files:
    root = ET.parse(file).getroot()
    for case in root.iter("testcase"):
        name = case.get("classname", "")
        expected = ("reader" if name.startswith(prefix + "reader.")
                    else "ui" if name.startswith(prefix) else "core")
        if expected != shard:
            raise SystemExit(f"Out-of-shard test {name} in {shard}: {file}")
        count += 1
        failures += sum(1 for child in case if child.tag in ("error", "failure"))
if count < 1 or failures:
    raise SystemExit(f"{shard}: {count} tests, {failures} failures")
print(f"{shard}: {count} executed test cases, zero failures; {len(files)} JUnit XML reports")
