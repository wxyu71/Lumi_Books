#!/usr/bin/env python3
"""Validate distribution-only invariants that Android/Gradle tools cannot express."""

from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"COMMUNITY DISTRIBUTION POLICY FAILED: {message}")


# These are packaging/distribution boundaries, not substitutes for behavioral tests.
for forbidden in (
    "app/src/premium",
    "app/src/main/java/com/huangder/lumibooks/ui/settings/SponsorActivity.kt",
    "app/src/main/java/com/huangder/lumibooks/data/github/GitHubContributorsClient.kt",
    "docs/CNAME",
):
    require(not (ROOT / forbidden).exists(), f"forbidden path present: {forbidden}")

config = json.loads((ROOT / "docs/app-config.json").read_text(encoding="utf-8"))
require(
    str(config.get("release_url", "")).startswith("https://github.com/wxyu71/Lumi_Books/"),
    "release_url must point to this community fork",
)
require(config.get("latest_version_code") == 0, "static config must not advertise an unpublished build")
require(
    config.get("update", {}).get("latest_version_code") == 0,
    "nested update config must not advertise an unpublished build",
)

properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
version = re.search(r"(?m)^lumi\.communityVersionName=(\S+)$", properties)
version_code = re.search(r"(?m)^lumi\.communityVersionCode=(\d+)$", properties)
require(version is not None, "community version name is missing")
require(version_code is not None and int(version_code.group(1)) > 0, "community versionCode must be positive")
require(
    re.fullmatch(r"\d+\.\d+\.\d+-beta\.\d+", version.group(1)) is not None,
    "community version must use X.Y.Z-beta.N",
)

changelog = (ROOT / "app/src/main/assets/changelog.md").read_text(encoding="utf-8").strip()
require(bool(changelog), "embedded changelog must not be empty")

print(f"Community distribution policy passed for {version.group(1)} (code {version_code.group(1)}).")
