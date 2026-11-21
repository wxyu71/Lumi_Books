#!/usr/bin/env python3
"""Validate community strings across supported languages without Android tooling."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
LOCALES = ("values", "values-en", "values-ja", "values-ko",
           "values-zh-rTW", "values-zh-rHK", "values-zh-rMO")
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sd]")
def load(folder):
    file = ROOT / "app/src/main/res" / folder / "community_strings.xml"
    root = ET.parse(file).getroot()
    entries = root.findall("string")
    names = [e.attrib["name"] for e in entries]
    if len(names) != len(set(names)):
        raise SystemExit(f"Duplicate resource names: {file}")
    return {e.attrib["name"]: (e.text or "") for e in entries}
base = load("values")
if not base:
    raise SystemExit("Empty community strings")
for locale in LOCALES:
    strings = load(locale)
    if strings.keys() != base.keys():
        missing = sorted(base.keys() - strings.keys())
        extra = sorted(strings.keys() - base.keys())
        raise SystemExit(f"{locale}: missing {missing}, unexpected {extra}")
    for key, value in strings.items():
        if not value.strip():
            raise SystemExit(f"{locale}: empty {key}")
        if sorted(PLACEHOLDER.findall(value)) != sorted(PLACEHOLDER.findall(base[key])):
            raise SystemExit(f"{locale}: placeholder mismatch: {key}")
    print(f"{locale}: {len(strings)} translated resources validated")
