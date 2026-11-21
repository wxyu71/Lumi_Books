pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Bypass intermittent Hilt plugin-marker lookup by resolving its
    // published implementation artifact directly. Keep version pinned.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.dagger.hilt.android" && requested.version != null) {
                useModule("com.google.dagger:hilt-android-gradle-plugin:${requested.version}")
            }
        }
    }

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EBookReader"
include(":app")
include(":benchmark")
include(":third_party:subsampling-scale-image-view")

include(":third_party:backdrop")
include(":third_party:word-converters")
