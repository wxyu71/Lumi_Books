"""Probe known Hilt repository endpoints without a real network connection."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "ci_hilt_artifact_probe.sh"

class HiltArtifactProbeTest(unittest.TestCase):
    def run_probe(self, statuses):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            shutil.copyfile(SCRIPT, root / "probe.sh")
            (root / "bin").mkdir()
            curl = root / "bin/curl"
            curl.write_text(
                "#!/usr/bin/env bash\n"
                "url=\"${@: -1}\"\n"
                "case \"$url\" in\n"
                "  *storage-download.googleapis.com*) printf \"$TEST_GCS\";;\n"
                "  *repo.maven.apache.org*) printf \"$TEST_APACHE\";;\n"
                "  *repo1.maven.org*) printf \"$TEST_REPO1\";;\n"
                "esac\n", encoding="utf-8",
            )
            curl.chmod(0o755)
            env = dict(os.environ, TEST_GCS=statuses[0],
                       TEST_APACHE=statuses[1], TEST_REPO1=statuses[2])
            env["PATH"] = str(root / "bin") + os.pathsep + env.get("PATH", "")
            env["GITHUB_STEP_SUMMARY"] = str(root / "summary")
            run = subprocess.run(["bash", "probe.sh"], cwd=root, env=env,
                                 capture_output=True, text=True, timeout=8)
            logs = (root / "test-logs/hilt-2.58-repositories.txt").read_text()
            summary = (root / "summary").read_text()
            return run.returncode, run.stdout, logs, summary

    def test_good_google_mirror_is_reported(self):
        rc, out, logs, summary = self.run_probe(("200", "404", "503"))
        self.assertEqual(0, rc)
        self.assertIn("HTTP 200", logs)
        self.assertIn("HTTP 404", logs)
        self.assertIn("HTTP 503", summary)
        self.assertNotIn("::warning::", out)

    def test_all_unavailable_diagnoses_but_does_not_suppress_gradle(self):
        rc, out, logs, summary = self.run_probe(("404", "404", "404"))
        self.assertEqual(0, rc)
        self.assertIn("::warning::", out)
        self.assertEqual(3, logs.count("HTTP 404"))
        self.assertIn("Hilt 2.58", summary)

if __name__ == "__main__":
    unittest.main()
