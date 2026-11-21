#!/usr/bin/env python3
"""Check that all app JVM test sources fit exactly one CI shard.

The same package boundaries are enforced at execution by app/build.gradle.kts.
No tests are silently discarded when new packages are added.
"""
from collections import Counter
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCES = (ROOT / "app/src/test/java", ROOT / "app/src/oss/test")
PATTERN = re.compile(r"(?m)^\s*package\s+([\w.]+)")
PREFIX = "com.huangder.lumibooks."
counts = Counter()
examples = {}
for source_root in SOURCES:
    if not source_root.exists():
        continue
    for path in sorted(source_root.rglob("*")):
        if not path.is_file() or path.suffix not in {".kt", ".java"}:
            continue
        if not path.stem.endswith(("Test", "Tests")):
            continue
        text = path.read_text("utf-8")
        match = PATTERN.search(text)
        if not match or not match.group(1).startswith(PREFIX):
            raise SystemExit(f"Unexpected JVM test package: {path}")
        pkg = match.group(1)
        if pkg.startswith(PREFIX + "ui.reader"):
            shard = "reader"
        elif pkg.startswith(PREFIX + "ui"):
            shard = "ui"
        else:
            shard = "core"
        counts[shard] += 1
        examples.setdefault(shard, path)
if sum(counts.values()) < 100 or any(counts[x] == 0 for x in ("reader", "ui", "core")):
    raise SystemExit(f"Missing JVM tests or shard: {dict(counts)}")
for shard in ("reader", "ui", "core"):
    print(f"{shard}: {counts[shard]} JVM test source files (e.g. {examples[shard].relative_to(ROOT)})")
print(f"All {sum(counts.values())} JVM test source files assigned exactly once.")
