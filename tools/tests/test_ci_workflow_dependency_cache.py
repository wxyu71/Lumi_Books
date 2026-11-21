"""Guard cache producer/consumer ordering and PR trust boundary."""
from pathlib import Path
import unittest

WORKFLOW = Path(__file__).resolve().parents[2] / ".github/workflows/android-beta.yml"

class WorkflowCacheHandoffTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        yml = WORKFLOW.read_text(encoding="utf-8")
        cls.all = yml
        cls.apk = yml.split("\n  apk:\n", 1)[1].split("\n  unit-tests:\n", 1)[0]
        cls.jvm = yml.split("\n  unit-tests:\n", 1)[1].split("\n  ci-gate:\n", 1)[0]
        cls.gate = yml.split("\n  ci-gate:\n", 1)[1]

    def test_only_same_repo_prs_and_main_write_cache(self):
        self.assertIn("cache-read-only: ${{ github.event_name == 'pull_request' && github.event.pull_request.head.repo.full_name != github.repository }}", self.apk)
        self.assertIn("cache-read-only: 'true'", self.jvm)
        self.assertNotIn("pull_request_target:", self.all)

    def test_reader_restores_cache_after_apk_saves(self):
        self.assertIn("needs: [policy, apk]", self.jvm)
        self.assertIn("if: ${{ always() && needs.policy.result == 'success' }}", self.jvm)

    def test_both_checks_remain_required(self):
        self.assertIn("needs: [policy, apk, unit-tests]", self.gate)
        self.assertIn('"$APK" == success && "$UNIT_TESTS" == success', self.gate)
        self.assertIn("bash tools/ci_run_jvm_shards.sh", self.jvm)
        self.assertNotIn("continue-on-error: true", self.jvm)

if __name__ == "__main__":
    unittest.main()
