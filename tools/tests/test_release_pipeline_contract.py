"""Personal Beta release must follow a successful main CI, without keystore setup."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
RELEASE = (ROOT / ".github/workflows/android-release.yml").read_text()
PR = (ROOT / ".github/workflows/android-beta.yml").read_text()
GRADLE = (ROOT / "app/build.gradle.kts").read_text()


class BetaPublicationContract(unittest.TestCase):
    def test_reuses_successful_main_ci_without_new_gate_or_tag_push(self):
        for value in ("workflow_run:", "workflows: ['Android Community Beta']",
                      "types: [completed]", "branches: [main]",
                      "workflow_run.conclusion == 'success'",
                      "workflow_run.event == 'push'",
                      "workflow_run.head_repository.full_name == github.repository",
                      "git rev-parse origin/main", "workflow_run.head_sha"):
            self.assertIn(value, RELEASE)
        self.assertNotIn("workflow_call:", PR)
        self.assertNotIn("needs: full-ci", RELEASE)
        self.assertNotIn("workflow_dispatch:", RELEASE)

    def test_no_signing_secret_or_long_term_key(self):
        self.assertNotIn("secrets.", RELEASE)
        self.assertNotIn("LUMI_BETA_KEYSTORE", RELEASE)
        self.assertNotIn("LUMI_RELEASE_KEYSTORE_PATH", GRADLE)
        self.assertIn("permissions:\n  contents: read", PR)
        self.assertIn("contents: write", RELEASE)
        self.assertIn("openssl rand -hex", RELEASE)
        self.assertIn("keytool -genkeypair", RELEASE)
        self.assertIn("zipalign", RELEASE)
        self.assertIn("apksigner", RELEASE)

    def test_publication_validation_and_user_warning(self):
        for value in ("versionCode=", "versionName=", "SHA256SUMS.txt", "--prerelease",
                      "--target", "gh release create", "gh release view",
                      "NOT an in-place APK update", "Back up your reading data"):
            self.assertIn(value, RELEASE)
        self.assertNotIn("[skip ci]", RELEASE)


if __name__ == "__main__":
    unittest.main()
