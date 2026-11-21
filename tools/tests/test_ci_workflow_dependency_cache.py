"""Prevent a return to duplicate Android toolchain installs and unverified test gaps."""
from pathlib import Path
import unittest

WORKFLOW = Path(__file__).resolve().parents[2] / ".github/workflows/android-beta.yml"

class WorkflowBuildAndTestContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        yml = WORKFLOW.read_text(encoding="utf-8")
        cls.workflow = yml
        cls.verify = yml.split("\n  verify:\n", 1)[1].split("\n  ci-gate:\n", 1)[0]
        cls.gate = yml.split("\n  ci-gate:\n", 1)[1]

    def test_single_jdk21_runner_prevents_duplicate_test_compilation(self):
        self.assertEqual(1, self.workflow.count("uses: ./.github/actions/setup-android-toolchain"))
        self.assertIn("java-version: '21'", self.verify)
        self.assertIn("bash tools/ci_run_jvm_shards.sh", self.verify)
        self.assertIn(":app:assembleDebug", self.verify)
        self.assertNotIn("\n  apk:\n", self.workflow)
        self.assertNotIn("\n  unit-tests:\n", self.workflow)

    def test_fork_pr_cannot_write_gradle_cache_or_access_release_secrets(self):
        self.assertIn("cache-read-only: ${{ github.event_name == 'pull_request' && github.event.pull_request.head.repo.full_name != github.repository }}", self.verify)
        self.assertIn("persist-credentials: false", self.verify)
        self.assertIn("permissions:\n  contents: read", self.workflow)
        self.assertNotIn("pull_request_target:", self.workflow)
        self.assertNotIn("secrets.", self.workflow)

    def test_apk_checksums_and_all_jvm_shards_required(self):
        self.assertIn("Verify APK metadata and create SHA-256 checksums", self.verify)
        self.assertIn("sha256sum ./*.apk", self.verify)
        self.assertIn("Run bounded JVM shards with separate reports", self.verify)
        self.assertIn("if: always()", self.verify)
        self.assertIn("needs: [policy, verify]", self.gate)
        self.assertIn('"$POLICY" == success && "$VERIFY" == success', self.gate)
        self.assertNotIn("continue-on-error: true", self.verify)

    def test_probes_and_failure_artifacts_are_retained(self):
        self.assertIn("bash tools/ci_hilt_artifact_probe.sh", self.verify)
        self.assertIn("Lumi-Community-APK-failure-", self.verify)
        self.assertIn("Lumi-Community-JVM-all-", self.verify)
        self.assertIn("Lumi-Community-Beta-", self.verify)

if __name__ == "__main__":
    unittest.main()
