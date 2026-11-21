"""Fast checks for the isolated, signed Beta release workflow."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
RELEASE = (ROOT / ".github/workflows/android-release.yml").read_text()
PR = (ROOT / ".github/workflows/android-beta.yml").read_text()
GRADLE = (ROOT / "app/build.gradle.kts").read_text()


class BetaPublicationContract(unittest.TestCase):
    def test_full_gate_before_tag_publication(self):
        self.assertIn("'v*-beta.*'", RELEASE)
        self.assertIn("uses: ./.github/workflows/android-beta.yml", RELEASE)
        self.assertIn("workflow_call:", PR)
        self.assertIn("needs: full-ci", RELEASE)
        self.assertIn("git merge-base --is-ancestor HEAD origin/main", RELEASE)
        self.assertIn("lumi.communityVersionName", RELEASE)

    def test_secrets_only_in_publisher(self):
        self.assertIn("permissions:\n  contents: read", PR)
        self.assertNotIn("secrets.", PR)
        self.assertIn("environment: beta-release", RELEASE)
        self.assertIn("contents: write", RELEASE)
        self.assertIn("LUMI_BETA_KEYSTORE_BASE64", RELEASE)
        self.assertIn("LUMI_BETA_CERT_SHA256", RELEASE)
        self.assertIn('signingConfig = signingConfigs.findByName("communityBeta")', GRADLE)
        self.assertIn("signingConfig = null", GRADLE)

    def test_fail_closed_artifact_publishing(self):
        for required in (":app:assembleRelease", "apksigner", "certificate SHA-256 digest",
                         "sha256sum --check SHA256SUMS.txt", "--verify-tag", "--prerelease",
                         "gh release create", "refusing to replace public assets"):
            self.assertIn(required, RELEASE)
        self.assertNotIn("[skip ci]", RELEASE)


if __name__ == "__main__":
    unittest.main()
