#!/usr/bin/env python3
"""Reject empty/misrouted JUnit runs and guard against accidental mass skipping."""

from pathlib import Path
import os
import sys
import xml.etree.ElementTree as ET

SHARDS = ("reader", "ui", "core")
MIN_EXECUTED = {
    "reader": 900,
    "ui": 600,
    "core": 900,
}
MAX_SKIPPED = {
    "modern": {"reader": 250, "ui": 250, "core": 250},
    "legacy": {"reader": 20, "ui": 20, "core": 20},
}


def lane_from_env():
    return "legacy" if os.getenv("LUMI_ROBOLECTRIC_LEGACY") == "1" else "modern"


def verify(shard, folder, lane):
    if shard not in SHARDS:
        raise ValueError(f"Unknown shard: {shard}")
    if lane not in MAX_SKIPPED:
        raise ValueError(f"Unknown lane: {lane}")

    files = sorted(Path(folder).glob("TEST-*.xml"))
    if not files:
        raise ValueError(f"No JUnit XML reports found for {shard}")

    prefix = "com.huangder.lumibooks.ui."
    executed = 0
    skipped = 0
    failures = 0
    for file in files:
        root = ET.parse(file).getroot()
        for case in root.iter("testcase"):
            name = case.get("classname", "")
            expected = (
                "reader" if name.startswith(prefix + "reader.")
                else "ui" if name.startswith(prefix)
                else "core"
            )
            if expected != shard:
                raise ValueError(f"Out-of-shard test {name} in {shard}: {file}")
            if case.find("skipped") is not None:
                skipped += 1
            else:
                executed += 1
            failures += sum(1 for child in case if child.tag in ("error", "failure"))

    if failures:
        raise ValueError(
            f"{shard} ({lane}): {executed} executed, {skipped} skipped, {failures} failures"
        )
    if executed < MIN_EXECUTED[shard]:
        raise ValueError(
            f"{shard} ({lane}): only {executed} executed; minimum is "
            f"{MIN_EXECUTED[shard]} (skipped={skipped})"
        )
    if skipped > MAX_SKIPPED[lane][shard]:
        raise ValueError(
            f"{shard} ({lane}): {skipped} skipped exceeds budget "
            f"{MAX_SKIPPED[lane][shard]} (executed={executed})"
        )
    return executed, skipped, failures, len(files)


def main(argv=None):
    argv = sys.argv if argv is None else argv
    if len(argv) != 3 or argv[1] not in SHARDS:
        raise SystemExit("Usage: ci_verify_junit.py <reader|ui|core> <result-directory>")
    shard = argv[1]
    lane = lane_from_env()
    try:
        executed, skipped, failures, reports = verify(shard, argv[2], lane)
    except (ValueError, ET.ParseError) as exc:
        raise SystemExit(str(exc)) from exc
    print(
        f"{shard} ({lane}): {executed} executed, {skipped} JUnit-reported skipped, "
        f"zero failures; {reports} JUnit XML reports; "
        f"minimum executed={MIN_EXECUTED[shard]}, max skipped={MAX_SKIPPED[lane][shard]}"
    )


if __name__ == "__main__":
    main()
