"""Protect fail-fast stage ordering, immutable actions and CI scope."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
CI = (ROOT / ".github/workflows/android-beta.yml").read_text(encoding="utf-8")
EXTENDED = (ROOT / ".github/workflows/android-emulator-extended.yml").read_text(encoding="utf-8")


def job(name, next_name):
    return CI.split(f"\n  {name}:\n", 1)[1].split(f"\n  {next_name}:\n", 1)[0]


class WorkflowBuildAndTestContract(unittest.TestCase):
    def test_p0_and_p1_p2_precede_every_package_job(self):
        self.assertIn("rhysd/actionlint@sha256:b1934ee5f1c509618f2508e6eb47ee0d3520686341fec936f3b79331f9315667", CI)
        self.assertIn("-shellcheck='shellcheck -S warning'", CI)
        self.assertIn("shellcheck --severity=error", CI)

        verify = job("verify", "quality")
        for check in (
            ":app:compileDebugKotlin",
            ":app:compileDebugUnitTestKotlin",
            ":app:prepareRobolectricSdkJars",
            "Smoke-test Robolectric API 33",
            "bash tools/ci_run_jvm_shards.sh",
        ):
            self.assertIn(check, verify)
        self.assertNotIn(":app:assembleDebug", verify)
        self.assertNotIn(":app:lintDebug", verify)

        quality = job("quality", "apk")
        self.assertIn("needs: [policy, verify]", quality)
        self.assertIn(":app:lintDebug", quality)
        self.assertNotIn(":app:assemble", quality)

        for name, next_name, task in (
            ("apk", "diagnostic", ":app:assembleDebug"),
            ("diagnostic", "release", ":app:assembleDiagnostic"),
            ("release", "device-smoke", ":app:assembleRelease"),
        ):
            section = job(name, next_name)
            self.assertIn("needs: [policy, verify, quality]", section)
            self.assertIn(task, section)

    def test_diagnostic_and_release_are_independent_jobs(self):
        diagnostic = job("diagnostic", "release")
        release = job("release", "device-smoke")
        self.assertNotIn(":app:assembleRelease", diagnostic)
        self.assertNotIn(":app:assembleDiagnostic", release)
        self.assertIn("Lumi-Community-Diagnostic-", diagnostic)
        self.assertIn("Lumi-Community-Release-", release)

    def test_p4_and_summary_wait_for_all_p3_results(self):
        device = job("device-smoke", "ci-gate")
        self.assertIn(
            "needs: [policy, verify, quality, apk, diagnostic, release]",
            device,
        )
        self.assertIn("api: [33, 35]", device)
        gate = CI.split("\n  ci-gate:\n", 1)[1]
        self.assertIn(
            "needs: [policy, verify, quality, apk, diagnostic, release, device-smoke]",
            gate,
        )
        for token in ("$POLICY", "$VERIFY", "$QUALITY", "$APK", "$DIAGNOSTIC", "$RELEASE", "$DEVICE"):
            self.assertIn(token, gate)
        self.assertIn("Historical explicit SDK cases are not claimed here", gate)

    def test_modern_and_legacy_robolectric_scope_is_explicit(self):
        self.assertIn("API 33/35/36 JVM scope", CI)
        self.assertIn("api: [26, 33, 36]", EXTENDED)
        self.assertIn("LUMI_ROBOLECTRIC_LEGACY: '1'", EXTENDED)
        self.assertIn("bash tools/ci_run_jvm_shards.sh", EXTENDED)

    def test_fork_is_unprivileged_and_only_main_apk_job_can_write_cache(self):
        self.assertIn("permissions:\n  contents: read", CI)
        self.assertIn("persist-credentials: false", CI)
        self.assertNotIn("pull_request_target:", CI)
        self.assertNotIn("secrets.", CI)
        self.assertIn(
            "cache-read-only: ${{ github.event_name != 'push' || github.ref != 'refs/heads/main' }}",
            CI,
        )
        self.assertIn("Android Debug", CI)
        self.assertIn("apksigner", CI)
        self.assertIn("sha256sum --check SHA256SUMS.txt", CI)
        self.assertNotIn("continue-on-error: true", CI)

    def test_bespoke_dependency_retry_and_probes_stay_retired(self):
        self.assertNotIn("ci_gradle_retry", CI)
        self.assertNotIn("ci_dependency_failure", CI)
        self.assertNotIn("ci_hilt_artifact_probe", CI)
        self.assertNotIn("ci_compose_artifact_probe", CI)

    def test_external_actions_use_immutable_commit_pins(self):
        external = re.findall(r"(?m)^\s*uses:\s+(?!\.\/)([^\s#]+)", CI)
        self.assertTrue(external)
        for item in external:
            self.assertRegex(item, r"^[^@]+@[0-9a-f]{40}$")


if __name__ == "__main__":
    unittest.main()
