"""No physical devices, external cloud farms, provider tokens or offline gates."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
CI = ROOT / ".github/workflows"
WORKFLOWS = ("android-beta.yml", "android-emulator-extended.yml", "android-quality.yml")

class GitHubOnlyAndroidCIContract(unittest.TestCase):
    def test_all_android_test_jobs_run_within_github_actions(self):
        self.assertFalse((CI / "android-device-smoke.yml").exists())
        for name in WORKFLOWS:
            with self.subTest(name=name):
                data = (CI / name).read_text(encoding="utf-8")
                self.assertIn("runs-on: ubuntu-24.04", data)
                self.assertNotRegex(data.lower(), r"firebase.?test.?lab|browserstack|aws.?device.?farm|sauce.?labs|perfecto|kobiton")
                self.assertNotIn("secrets.", data)
                self.assertNotIn("pull_request_target:", data)

    def test_required_matrix_and_png_remain_blocking(self):
        required = (CI / "android-beta.yml").read_text(encoding="utf-8")
        self.assertIn("api: [29, 35]", required)
        self.assertIn("Lumi-Community-Emulator-API", required)
        self.assertIn("bash tools/ci_run_device_smoke.sh", required)
        self.assertIn("needs: [policy, verify, quality, device-smoke]", required)
        emulator = (ROOT / "tools/ci_run_device_smoke.sh").read_text()
        self.assertIn("icon-style-before.png", emulator)
        self.assertIn("icon-style-after.png", emulator)
        self.assertIn("ci_verify_android_junit.py", emulator)

if __name__ == "__main__":
    unittest.main()
