"""Guard the pinned Hilt plugin against repository regressions and AGP 9 jumps."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class HiltRepositoryRoutingTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
        cls.root_build = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
        cls.app_build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")

    def test_hilt_version_is_pinned_and_matches_runtime_and_compiler(self):
        self.assertIn('id("com.google.dagger.hilt.android") version "2.58" apply false', self.root_build)
        self.assertIn('implementation("com.google.dagger:hilt-android:2.58")', self.app_build)
        self.assertIn('ksp("com.google.dagger:hilt-android-compiler:2.58")', self.app_build)

    def test_official_fallback_scoped_to_dagger_not_entire_dependency_graph(self):
        self.assertEqual(2, self.settings.count('maven(url = "https://repo1.maven.org/maven2")'))
        self.assertEqual(4, self.settings.count('includeGroup("com.google.dagger")'))
        self.assertEqual(2, self.settings.count('maven(url = "https://maven-central.storage-download.googleapis.com/maven2")'))
        self.assertIn('includeGroup("com.google.dagger.hilt.android")', self.settings)
        self.assertNotIn("allowInsecureProtocol", self.settings)
        self.assertNotIn("mavenLocal()", self.settings)
        self.assertIn("mavenCentral()", self.settings)
        self.assertIn("google()", self.settings)
        self.assertIn("gradlePluginPortal()", self.settings)
        self.assertIn('useModule("com.google.dagger:hilt-android-gradle-plugin:${requested.version}")', self.settings)

    def test_robolectric_runtime_mirrors_are_scoped_to_jvm_test_group(self):
        # Avoid a blanket mirror; only the published Robolectric group gets
        # alternate Maven Central endpoints if the canonical CDN is throttled.
        self.assertEqual(1, self.settings.count('includeGroup("org.robolectric")'))
        self.assertNotIn('includeGroupByRegex("org[.]robolectric[.].*")', self.settings)

    def test_modern_pr_sdks_are_small_and_legacy_is_explicit(self):
        self.assertIn('it.systemProperty("robolectric.enabledSdks", "33,35,36")',
                      self.app_build)
        self.assertIn('LUMI_ROBOLECTRIC_LEGACY', self.app_build)
        main, legacy = self.app_build.split("val modernRobolectricSdks = listOf(", 1)[1].split(
            "val legacyRobolectricSdks = listOf(", 1)
        modern_versions = ("13-robolectric-9030017-i7",
                           "15-robolectric-13954326-i7",
                           "16-robolectric-13921718-i7")
        for version in modern_versions:
            self.assertIn('"' + version + '"', main)
        self.assertEqual(3, main.count("-robolectric-"))
        self.assertEqual(7, legacy.split("val robolectricSdkVersions", 1)[0].count("-robolectric-"))
        self.assertIn('configurations.create("robolectricSdkJar$index")',
                      self.app_build)
        self.assertIn('isTransitive = false', self.app_build)
        self.assertIn('from(robolectricSdkJarConfigurations)', self.app_build)
        self.assertIn('dependsOn(prepareRobolectricSdkJars)', self.app_build)
        self.assertIn('it.systemProperty("robolectric.dependency.dir",', self.app_build)
        self.assertNotIn('it.systemProperty("robolectric.dependency.repo.url"', self.app_build)

    def test_jetbrains_compose_mirrors_are_limited_to_compose_groups(self):
        self.assertEqual(2, self.settings.count(
            'includeGroupByRegex("org[.]jetbrains[.]compose[.].*")'
        ))
        self.assertNotIn('includeGroupByRegex(".*")', self.settings)

if __name__ == "__main__":
    unittest.main()
