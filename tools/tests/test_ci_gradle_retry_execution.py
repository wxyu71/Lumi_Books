"""Exercise Gradle's bounded retry wrapper without requiring Android or network."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / "tools"


class GradleRetryExecutionTest(unittest.TestCase):
    def run_fake(self, first_failure: str):
        with tempfile.TemporaryDirectory(prefix="lumi-gradle-retry-") as temporary:
            root = Path(temporary)
            (root / "tools").mkdir()
            (root / "bin").mkdir()
            for name in ("ci_gradle_retry.sh", "ci_dependency_failure.py"):
                shutil.copyfile(TOOLS / name, root / "tools" / name)
            gradle = root / "gradlew"
            gradle.write_text(
                "#!/usr/bin/env bash\n"
                "printf '%s\\n' \"$*\" >> invocations.log\n"
                "if [[ \" $* \" == *' --refresh-dependencies '* ]]; then\n"
                "  echo BUILD_SUCCESSFUL\n"
                "  exit 0\n"
                "fi\n"
                "printf '%s\\n' \"$TEST_FAKE_FAILURE\"\n"
                "exit 1\n",
                encoding="utf-8",
            )
            gradle.chmod(0o755)
            no_sleep = root / "bin" / "sleep"
            no_sleep.write_text("#!/bin/sh\nexit 0\n", encoding="utf-8")
            no_sleep.chmod(0o755)
            env = dict(os.environ, TEST_FAKE_FAILURE=first_failure)
            env["PATH"] = str(root / "bin") + os.pathsep + env.get("PATH", "")
            proc = subprocess.run(
                ["bash", "tools/ci_gradle_retry.sh", "logs/attempts.log", ":app:assembleDebug"],
                cwd=root, env=env, text=True, capture_output=True, timeout=20,
            )
            calls = (root / "invocations.log").read_text(encoding="utf-8").splitlines()
            diagnostics = (root / "logs" / "attempts.log").read_text(encoding="utf-8")
            return proc.returncode, calls, diagnostics

    def test_verified_published_hilt_plugin_refreshes_metadata(self):
        failure = (
            "Plugin [id: 'com.google.dagger.hilt.android', version: '2.58', "
            "artifact: 'com.google.dagger:hilt-android-gradle-plugin:2.58', apply: false] "
            "was not found in any of the following sources: "
            "could not resolve plugin artifact 'com.google.dagger:hilt-android-gradle-plugin:2.58'"
        )
        rc, calls, log = self.run_fake(failure)
        self.assertEqual(0, rc, log)
        self.assertEqual(2, len(calls))
        self.assertNotIn("--refresh-dependencies", calls[0])
        self.assertIn("--refresh-dependencies", calls[1])
        self.assertIn("retrying with --refresh-dependencies", log)

    def test_robolectric_429_in_unit_test_classpath_is_retried(self):
        failure = (
            "> Task :app:hiltAggregateDepsDebugUnitTest FAILED\n"
            "Could not resolve all files for configuration ':app:debugUnitTestRuntimeClasspath'.\n"
            "Could not GET 'https://repo.maven.apache.org/maven2/org/robolectric/"
            "nativeruntime-dist-compat/1.0.18/nativeruntime-dist-compat-1.0.18.jar'. "
            "Received status code 429 from server: Too Many Requests\n"
            "BUILD FAILED in 1m 15s"
        )
        rc, calls, log = self.run_fake(failure)
        self.assertEqual(0, rc, log)
        self.assertEqual(2, len(calls))
        self.assertIn("--refresh-dependencies", calls[1])

    def test_compiler_failure_remains_fatal_without_retry(self):
        rc, calls, _ = self.run_fake("e: file:///app/src/main/Kotlin.kt:1:1 compilation failed")
        self.assertNotEqual(0, rc)
        self.assertEqual(1, len(calls))

    def test_failing_junit_assertions_are_not_retried(self):
        rc, calls, _ = self.run_fake(
            "100 tests completed, 1 failed\nThere were failing tests"
        )
        self.assertNotEqual(0, rc)
        self.assertEqual(1, len(calls))

    def test_unknown_plugin_remains_fatal_without_retry(self):
        rc, calls, _ = self.run_fake("Plugin [id: 'example.invalid', version: '1.0'] was not found")
        self.assertNotEqual(0, rc)
        self.assertEqual(1, len(calls))


if __name__ == "__main__":
    unittest.main()
