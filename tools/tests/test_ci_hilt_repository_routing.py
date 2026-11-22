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

    def test_robolectric_sdk_is_prefetched_before_any_jvm_test(self):
        # #133: runtime Maven 429s; #134: the first offline SDK worked,
        # but 159 tests needed nine additional Android SDK versions.
        versions = (
            "8.0.0_r4-robolectric-r1-i7",
            "9-robolectric-4913185-2-i7",
            "10-robolectric-5803371-i7",
            "11-robolectric-6757853-i7",
            "12-robolectric-7732740-i7",
            "12.1-robolectric-8229987-i7",
            "13-robolectric-9030017-i7",
            "14-robolectric-10818077-i7",
            "15-robolectric-13954326-i7",
            "16-robolectric-13921718-i7",
        )
        for version in versions:
            self.assertIn('"' + version + '"', self.app_build)
        self.assertIn('val robolectricSdkJarConfigurations = robolectricSdkVersions.mapIndexed',
                      self.app_build)
        self.assertIn('configurations.create("robolectricSdkJar$index")', self.app_build)
        self.assertIn('isTransitive = false', self.app_build)
        self.assertIn('org.robolectric:android-all-instrumented:$version@jar',
                      self.app_build)
        self.assertIn('from(robolectricSdkJarConfigurations)', self.app_build)
        self.assertIn('val prepareRobolectricSdkJars = tasks.register<Copy>', self.app_build)
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
