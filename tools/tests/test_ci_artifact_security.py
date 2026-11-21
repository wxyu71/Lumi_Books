"""Fast regression checks for PR artifact signing, publication and report isolation."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class ArtifactSecurityContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.required = (ROOT / ".github/workflows/android-beta.yml").read_text()
        cls.extended = (ROOT / ".github/workflows/android-device-smoke.yml").read_text()
        cls.quality = (ROOT / ".github/workflows/android-quality.yml").read_text()

    def test_only_debug_signed_apks_are_published_from_pr(self):
        build = self.required
        self.assertIn("apksigner", build)
        self.assertIn("verify --verbose --print-certs", build)
        self.assertIn("Android Debug", build)
        self.assertIn("sha256sum --check SHA256SUMS.txt", build)
        self.assertIn("app/build/oss/outputs/apk/debug/*universal*.apk", build)
        self.assertIn("path: dist/", build)
        self.assertNotIn("path: app/build/oss/outputs/apk/release", build)
        self.assertNotIn("secrets.", build)
        self.assertIn("persist-credentials: false", build)
        self.assertIn("permissions:\n  contents: read", build)

    def test_weekly_emulator_matrix_remains_separate_and_nonblocking(self):
        self.assertIn("api: [26, 33, 36]", self.extended)
        self.assertIn("LUMI_DEVICE_EXTENDED=1", self.extended)
        self.assertIn("bash tools/ci_run_device_smoke.sh", self.extended)
        self.assertNotIn("pull_request:", self.extended)
        self.assertIn("workflow_dispatch:", self.extended)

    def test_weekly_quality_scans_other_variants(self):
        self.assertIn(":app:lintRelease :app:lintDiagnostic", self.quality)
        self.assertNotIn("pull_request:", self.quality)

    def test_visual_regression_exports_png_and_verifies_required_classes(self):
        runner = (ROOT / "tools/ci_run_device_smoke.sh").read_text()
        verifier = (ROOT / "tools/ci_verify_android_junit.py").read_text()
        capture = (ROOT / "app/src/androidTest/java/com/huangder/lumibooks/ui/settings/AppIconScreenshotInstrumentedTest.kt").read_text()
        self.assertIn("MediaStore.Images.Media.RELATIVE_PATH", capture)
        self.assertIn("/sdcard/Pictures/LumiCI", runner)
        self.assertIn("Screenshot PNGs were not exported", runner)
        self.assertIn('"$test_classes"', runner)
        self.assertIn("required_classes", verifier)

if __name__ == "__main__":
    unittest.main()
