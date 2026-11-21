"""Ensure emulator JUnit cannot pass when tests are missing, broken or failed."""
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ci_verify_android_junit import inspect_reports

class InstrumentedJUnitVerifierTest(unittest.TestCase):
    def test_zero_xml_is_fatal(self):
        with tempfile.TemporaryDirectory() as temp:
            cases, failures, skipped, errors = inspect_reports(Path(temp), 1)
            self.assertEqual((0, 0, 0), (cases, failures, skipped))
            self.assertTrue(errors)

    def test_instrumentation_results_count_real_testcases(self):
        with tempfile.TemporaryDirectory() as temp:
            reports = Path(temp) / "connected/debug/pixel6"
            reports.mkdir(parents=True)
            (reports / "TEST-com.example.SmokeTest.xml").write_text(
                '<testsuite tests="3"><testcase classname="example" name="one"/>'
                '<testcase classname="example" name="two"/>'
                '<testcase classname="example" name="three"><skipped/></testcase></testsuite>',
                encoding="utf-8"
            )
            self.assertEqual((3, 0, 1, []), inspect_reports(Path(temp), 3))
            self.assertTrue(inspect_reports(Path(temp), 4)[3])

    def test_test_failure_and_bad_xml_are_fatal(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / "TEST-failed.xml").write_text(
                '<testsuite><testcase classname="example" name="broken">'
                '<failure message="wrong pixels"/></testcase></testsuite>', encoding="utf-8"
            )
            (root / "TEST-malformed.xml").write_text("<testsuite", encoding="utf-8")
            cases, failures, _, errors = inspect_reports(root, 1)
            self.assertEqual(1, cases)
            self.assertEqual(1, failures)
            self.assertGreaterEqual(len(errors), 2)
            self.assertIn("wrong pixels", " ".join(errors))

    def test_xml_name_must_have_test_prefix(self):
        with tempfile.TemporaryDirectory() as temp:
            (Path(temp) / "unrelated.xml").write_text(
                '<testsuite><testcase name="not-an-instrumented-test"/></testsuite>',
                encoding="utf-8"
            )
            self.assertTrue(inspect_reports(Path(temp), 1)[3])

if __name__ == "__main__":
    unittest.main()
