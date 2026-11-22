"""Tests for lane-aware JVM JUnit completeness guardrails."""
from pathlib import Path
import tempfile
import unittest

from tools.ci_verify_junit import MAX_SKIPPED, MIN_EXECUTED, verify


def write_suite(folder, shard, executed, skipped=0, failures=0):
    if shard == "reader":
        classname = "com.huangder.lumibooks.ui.reader.ReaderBudgetTest"
    elif shard == "ui":
        classname = "com.huangder.lumibooks.ui.settings.UiBudgetTest"
    else:
        classname = "com.huangder.lumibooks.data.CoreBudgetTest"
    cases = [f'<testcase classname="{classname}" name="ok{i}"/>' for i in range(executed)]
    cases += [
        f'<testcase classname="{classname}" name="skip{i}"><skipped/></testcase>'
        for i in range(skipped)
    ]
    cases += [
        f'<testcase classname="{classname}" name="fail{i}"><failure message="boom"/></testcase>'
        for i in range(failures)
    ]
    Path(folder, "TEST-budget.xml").write_text(
        "<testsuite>" + "".join(cases) + "</testsuite>",
        encoding="utf-8",
    )


class JunitCompletenessBudgetTest(unittest.TestCase):
    def run_case(self, shard, lane, executed, skipped=0, failures=0):
        with tempfile.TemporaryDirectory() as temp:
            write_suite(temp, shard, executed, skipped, failures)
            return verify(shard, temp, lane)

    def test_modern_lane_accepts_expected_scale_and_visible_legacy_skips(self):
        executed = MIN_EXECUTED["reader"]
        skipped = MAX_SKIPPED["modern"]["reader"]
        result = self.run_case("reader", "modern", executed, skipped)
        self.assertEqual((executed, skipped, 0, 1), result)

    def test_execution_collapse_fails(self):
        with self.assertRaisesRegex(ValueError, "minimum"):
            self.run_case("ui", "modern", MIN_EXECUTED["ui"] - 1)

    def test_mass_skipping_fails_in_legacy_lane(self):
        with self.assertRaisesRegex(ValueError, "exceeds budget"):
            self.run_case(
                "core",
                "legacy",
                MIN_EXECUTED["core"],
                MAX_SKIPPED["legacy"]["core"] + 1,
            )

    def test_failures_are_never_hidden_by_budgets(self):
        with self.assertRaisesRegex(ValueError, "1 failures"):
            self.run_case("reader", "modern", MIN_EXECUTED["reader"], failures=1)


if __name__ == "__main__":
    unittest.main()
