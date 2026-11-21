"""The lint debt budget is immutable, exact per issue-location and fail-closed."""
import hashlib
from pathlib import Path
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ci_verify_lint_debt import (fingerprint, read_legacy_snapshot, verify,
                                 EXPECTED_LEGACY_RECORDS)

ROOT = Path(__file__).resolve().parents[2]

class LintLegacyGateTest(unittest.TestCase):
    def test_tracked_debt_snapshot_is_present_and_immutable(self):
        hashes = read_legacy_snapshot(ROOT / "tools/ci_lint_legacy_fingerprints.bin")
        self.assertEqual(EXPECTED_LEGACY_RECORDS, sum(hashes.values()))
        self.assertGreater(len(hashes), 500)

    def test_new_lint_error_blocks_even_if_old_errors_were_fixed(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "lint-results-debug.xml"
            path.write_text(
                '<issues format="6"><issue id="NewApi" severity="Error" message="fresh bug">'
                '<location file="src/main/java/FreshBug.kt" line="12" column="7"/>'
                '</issue></issues>', encoding="utf-8")
            errors, locations, found = verify(path, read_legacy_snapshot(
                ROOT / "tools/ci_lint_legacy_fingerprints.bin"))
            self.assertEqual((1, 1), (errors, locations))
            self.assertEqual(1, len(found))
            self.assertIn("FreshBug.kt", found[0])

    def test_more_than_baseline_duplicate_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "lint-results-debug.xml"
            path.write_text(
                '<issues><issue id="Any" severity="Error" message="x">'
                '<location file="src/main/java/Demo.kt" line="1" column="1"/>'
                '</issue></issues>', encoding="utf-8")
            record = next(ET.parse(path).getroot().iter("issue"))
            key = fingerprint(record, record.find("location"))
            from collections import Counter
            budget = Counter({key: 1})
            self.assertFalse(verify(path, budget)[2])
            path.write_text(
                '<issues><issue id="Any" severity="Error" message="x">'
                '<location file="src/main/java/Demo.kt" line="1" column="1"/>'
                '<location file="src/main/java/Demo.kt" line="1" column="1"/>'
                '</issue></issues>', encoding="utf-8")
            self.assertEqual(1, len(verify(path, budget)[2]))

    def test_wrong_snapshot_or_missing_lint_xml_cannot_pass(self):
        with tempfile.TemporaryDirectory() as tmp:
            invalid = Path(tmp) / "legacy.bin"
            invalid.write_bytes(bytes(EXPECTED_LEGACY_RECORDS * 8))
            with self.assertRaises(ValueError):
                read_legacy_snapshot(invalid)
            with self.assertRaises(FileNotFoundError):
                verify(Path(tmp) / "missing.xml", {})

if __name__ == "__main__":
    unittest.main()
