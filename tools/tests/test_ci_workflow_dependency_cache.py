"""Protect early failures, signed artifacts and blocking API 33+ CI checks."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
CI = (ROOT / ".github/workflows/android-beta.yml").read_text(encoding="utf-8")
EXTENDED = (ROOT / ".github/workflows/android-emulator-extended.yml").read_text(encoding="utf-8")

class WorkflowBuildAndTestContract(unittest.TestCase):
    def test_fast_checks_and_compilation_precede_apk_packaging(self):
        self.assertIn("rhysd/actionlint@sha256:b1934ee5f1c509618f2508e6eb47ee0d3520686341fec936f3b79331f9315667", CI)
        self.assertIn("-shellcheck='shellcheck -S warning'", CI)
        self.assertIn("shellcheck --severity=error", CI)
        verify = CI.split("\n  verify:\n", 1)[1].split("\n  quality:\n", 1)[0]
        for check in (":app:compileDebugKotlin", ":app:compileDebugUnitTestKotlin",
                      ":app:prepareRobolectricSdkJars", "Smoke-test Robolectric API 33",
                      "bash tools/ci_run_jvm_shards.sh", ":app:assembleDebug"):
            self.assertIn(check, verify)
        self.assertLess(verify.index(":app:compileDebugUnitTestKotlin"),
                        verify.index("bash tools/ci_run_jvm_shards.sh"))
        self.assertLess(verify.index("bash tools/ci_run_jvm_shards.sh"),
                        verify.index(":app:assembleDebug"))
        self.assertEqual(1, verify.count("uses: ./.github/actions/setup-android-toolchain"))
        self.assertIn("java-version: '21'", verify)
        self.assertNotIn("ci_gradle_retry", CI)
        self.assertNotIn("ci_hilt_artifact_probe", CI)
        self.assertNotIn("ci_compose_artifact_probe", CI)

    def test_fork_is_unprivileged_and_apks_debug_signed_only(self):
        self.assertIn("permissions:\n  contents: read", CI)
        self.assertIn("persist-credentials: false", CI)
        self.assertNotIn("pull_request_target:", CI)
        self.assertNotIn("secrets.", CI)
        self.assertIn("cache-read-only: ${{ github.event_name == 'pull_request' && github.event.pull_request.head.repo.full_name != github.repository }}", CI)
        self.assertIn("Android Debug", CI)
        self.assertIn("apksigner", CI)
        self.assertIn("sha256sum --check SHA256SUMS.txt", CI)

    def test_platform_matrix_and_all_required_gates(self):
        self.assertIn("api: [33, 35]", CI)
        self.assertIn("api: [26, 33, 36]", EXTENDED)
        self.assertIn("LUMI_ROBOLECTRIC_LEGACY: '1'", EXTENDED)
        self.assertIn("bash tools/ci_run_jvm_shards.sh", EXTENDED)
        self.assertIn("needs: [policy, verify, quality]", CI)
        self.assertIn("needs: [policy, verify, quality, device-smoke]", CI)
        self.assertIn('"$POLICY" == success && "$VERIFY" == success && "$QUALITY" == success && "$DEVICE" == success', CI)
        self.assertIn(":app:lintDebug", CI)
        self.assertIn(":app:assembleDiagnostic", CI)
        self.assertIn(":app:assembleRelease", CI)
        self.assertIn("Lumi-Community-JVM-all-", CI)
        self.assertIn("Lumi-Community-Quality-", CI)
        self.assertIn("Lumi-Community-Emulator-API", CI)
        self.assertNotIn("continue-on-error: true", CI)

    def test_external_actions_use_immutable_commit_pins(self):
        external = re.findall(r"(?m)^\s*uses:\s+(?!\.\/)([^\s#]+)", CI)
        self.assertTrue(external)
        for item in external:
            self.assertRegex(item, r"^[^@]+@[0-9a-f]{40}$")

if __name__ == "__main__":
    unittest.main()
