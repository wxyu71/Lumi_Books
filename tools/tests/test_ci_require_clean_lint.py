"""Full Android Lint must be clean; no legacy errors are exempt."""
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ci_require_clean_lint import verify


class ZeroErrorLintGateTest(unittest.TestCase):
    def parse(self, content):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "lint-results.xml"
            path.write_text(content, encoding="utf-8")
            return verify(path)

    def test_only_warnings_and_hints_are_allowed(self):
        self.assertEqual((0, 1, 1, []), self.parse(
            '<issues format="6"><issue id="Warn" severity="Warning"/>'
            '<issue id="Hint" severity="Informational"/></issues>'))

    def test_old_and_new_errors_fail_identically(self):
        errors, warnings, hints, details = self.parse(
            '<issues format="6"><issue id="MissingTranslation" severity="Error" '
            'message="legacy"><location file="app/src/main/res/values/strings.xml" line="1"/></issue>'
            '<issue id="NewApi" severity="Fatal" message="new"/>'
            '</issues>')
        self.assertEqual((errors, warnings, hints), (2, 0, 0))
        self.assertIn("MissingTranslation", details[0])
        self.assertIn("NewApi", details[1])

    def test_malformed_and_missing_reports_fail_closed(self):
        from xml.etree.ElementTree import ParseError
        with self.assertRaises(ParseError):
            self.parse("<issues")
        with self.assertRaises(ValueError):
            self.parse('<anything format="6"/>')
        with self.assertRaises(ValueError):
            self.parse('<issues/>')
        with self.assertRaises(FileNotFoundError):
            verify(Path("/does/not/exist/lint-results.xml"))


if __name__ == "__main__":
    unittest.main()
