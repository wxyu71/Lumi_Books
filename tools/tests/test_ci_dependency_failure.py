"""Dependency retry never turns Kotlin or test failures into a successful build."""
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ci_dependency_failure import is_retryable_resolution_failure

class CiDependencyFailureTest(unittest.TestCase):
    def test_published_hilt_258_may_be_rechecked_with_fresh_metadata(self):
        log = """Plugin [id: 'com.google.dagger.hilt.android', version: '2.58', apply: false] was not found in any of the following sources:
        Plugin Repositories (could not resolve plugin artifact 'com.google.dagger.hilt.android:com.google.dagger.hilt.android.gradle.plugin:2.58')"""
        self.assertTrue(is_retryable_resolution_failure(log))
        self.assertTrue(is_retryable_resolution_failure(log + "\nCould not GET https://repo.maven.apache.org/"))
        self.assertTrue(is_retryable_resolution_failure(
            "Plugin [id: 'com.google.dagger.hilt.android', version: '2.58', "
            "artifact: 'com.google.dagger:hilt-android-gradle-plugin:2.58', apply: false] "
            "was not found in any of the following sources: "
            "could not resolve plugin artifact 'com.google.dagger:hilt-android-gradle-plugin:2.58'"
        ))

    def test_network_errors(self):
        for detail in ("Could not GET 'https://repo.maven.apache.org/maven2'",
                       "Read timed out", "HTTP 503", "Connection reset"):
            with self.subTest(detail=detail):
                self.assertTrue(is_retryable_resolution_failure(detail))

    def test_robolectric_429_after_unit_test_task_failure_is_network_retryable(self):
        # Actual run #132: the Hilt test classpath needs a large published
        # Robolectric artifact, and Maven Central returned HTTP 429.
        log = """ > Task :app:hiltAggregateDepsDebugUnitTest FAILED
        Execution failed for task ':app:hiltAggregateDepsDebugUnitTest'.
        Could not resolve all files for configuration ':app:debugUnitTestRuntimeClasspath'.
        Could not download nativeruntime-dist-compat-1.0.18.jar
        (org.robolectric:nativeruntime-dist-compat:1.0.18)
        Could not GET 'https://repo.maven.apache.org/maven2/org/robolectric/nativeruntime-dist-compat/1.0.18/nativeruntime-dist-compat-1.0.18.jar'.
        Received status code 429 from server: Too Many Requests
        BUILD FAILED in 1m 15s"""
        self.assertTrue(is_retryable_resolution_failure(log))
        self.assertFalse(is_retryable_resolution_failure(
            "> Task :app:hiltAggregateDepsDebugUnitTest FAILED\\n"
            "BUILD FAILED in 1m 15s"
        ))
        self.assertFalse(is_retryable_resolution_failure(
            log + "\\n200 tests completed, 1 failed"
        ))

    def test_never_retry_compiler_test_or_success(self):
        for detail in ("e: file:///src/Book.kt:1:1 Unresolved reference",
                       "Compilation error. See log for more details",
                       "100 tests completed, 2 failed", "There were failing tests",
                       "AssertionError: expected true", "BUILD SUCCESSFUL"):
            with self.subTest(detail=detail):
                self.assertFalse(is_retryable_resolution_failure(detail))

    def test_compose_171_emulator_cache_misses_only_retry_in_instrumentation(self):
        error = (
            "Could not resolve all files for configuration "
            "':app:debugAndroidTestCompileClasspath'.\\n"
            "Could not find org.jetbrains.compose.runtime:runtime-saveable:1.7.1.\\n"
            "Searched in the following locations:\\n"
            "https://repo.maven.apache.org/maven2/"
        )
        self.assertTrue(is_retryable_resolution_failure(error))
        self.assertFalse(is_retryable_resolution_failure(error.replace(
            "debugAndroidTestCompileClasspath", "debugRuntimeClasspath"
        )))
        self.assertFalse(is_retryable_resolution_failure(error.replace("1.7.1.", "1.7.99.")))
        self.assertFalse(is_retryable_resolution_failure(
            error + "\\nThere were failing tests"
        ))

    def test_generic_misconfiguration_is_not_retryable(self):
        self.assertFalse(is_retryable_resolution_failure("Plugin [id: 'not.a.real.plugin'] was not found"))
        self.assertFalse(is_retryable_resolution_failure("Could not find method android() for arguments"))
        self.assertFalse(is_retryable_resolution_failure(
            "Plugin [id: 'com.google.dagger.hilt.android', version: '2.59', "
            "artifact: 'com.google.dagger:hilt-android-gradle-plugin:2.59'] "
            "was not found in any of the following sources:"
        ))
        self.assertFalse(is_retryable_resolution_failure(
            "Plugin [id: 'com.google.dagger.hilt.android', version: '2.58', "
            "artifact: 'com.google.dagger:hilt-android-gradle-plugin:2.58'] "
            "was not found in any of the following sources:\n"
            "Compilation error. See log for more details"
        ))
        self.assertFalse(is_retryable_resolution_failure(""))

if __name__ == "__main__":
    unittest.main()
