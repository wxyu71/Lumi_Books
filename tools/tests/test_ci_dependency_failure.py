"""Dependency retry never turns Kotlin or test failures into a successful build."""
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ci_dependency_failure import is_retryable_resolution_failure

class CiDependencyFailureTest(unittest.TestCase):
    def test_missing_hilt_marker_is_not_automatically_retryable(self):
        log = """Plugin [id: 'com.google.dagger.hilt.android', version: '2.58', apply: false] was not found
        Plugin Repositories (could not resolve plugin artifact 'com.google.dagger.hilt.android:com.google.dagger.hilt.android.gradle.plugin:2.58')"""
        self.assertFalse(is_retryable_resolution_failure(log))
        self.assertTrue(is_retryable_resolution_failure(log + "\nCould not GET https://repo.maven.apache.org/"))

    def test_network_errors(self):
        for detail in ("Could not GET 'https://repo.maven.apache.org/maven2'",
                       "Read timed out", "HTTP 503", "Connection reset"):
            with self.subTest(detail=detail):
                self.assertTrue(is_retryable_resolution_failure(detail))

    def test_never_retry_compiler_test_or_success(self):
        for detail in ("e: file:///src/Book.kt:1:1 Unresolved reference",
                       "Compilation error. See log for more details",
                       "100 tests completed, 2 failed", "There were failing tests",
                       "AssertionError: expected true", "BUILD SUCCESSFUL"):
            with self.subTest(detail=detail):
                self.assertFalse(is_retryable_resolution_failure(detail))

    def test_generic_misconfiguration_is_not_retryable(self):
        self.assertFalse(is_retryable_resolution_failure("Plugin [id: 'not.a.real.plugin'] was not found"))
        self.assertFalse(is_retryable_resolution_failure("Could not find method android() for arguments"))
        self.assertFalse(is_retryable_resolution_failure(""))

if __name__ == "__main__":
    unittest.main()
