"""Standard-library tests for CI JUnit summary diagnostics."""
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ci_junit_summary import render


class CiJunitSummaryTest(unittest.TestCase):
    def test_reports_failed_and_slowest_tests(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "TEST-a.xml").write_text(
                '<testsuite><testcase classname="a.ReaderTest" name="pass" time="0.2"/>'
                '<testcase classname="a.ReaderTest" name="broken" time="5.25">'
                '<failure message="bad &amp; error">stacktrace</failure></testcase></testsuite>',
                encoding="utf-8")
            message = render("reader", root, root / "missing.log")
            self.assertIn("2 testcases", message)
            self.assertIn("1 failures/errors", message)
            self.assertIn("5.25 s", message)
            self.assertIn("bad & error", message)

    def test_missing_xml_surfaces_compiler_failure(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            log = root / "gradle.log"
            log.write_text("compiling\ne: Unresolved reference widget\nBUILD FAILED\n", encoding="utf-8")
            result = render("ui", root / "reports", log)
            self.assertIn("No executable JUnit cases", result)
            self.assertIn("Unresolved reference widget", result)


if __name__ == "__main__":
    unittest.main()
