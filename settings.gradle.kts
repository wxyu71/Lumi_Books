pluginManagement {
    repositories {
        // Official Maven Central alternative endpoint. The canonical CDN
        // reported HTTP GET "Resource missing" for the released Hilt 2.58 POM
        // on multiple CI runs; keep the alternate scope limited to Dagger.
        maven(url = "https://repo1.maven.org/maven2") {
            content {
                includeGroup("com.google.dagger")
                includeGroup("com.google.dagger.hilt.android")
            }
        }
        // Independent Google Cloud-hosted mirror, restricted to Hilt only.
        maven(url = "https://maven-central.storage-download.googleapis.com/maven2") {
            content {
                includeGroup("com.google.dagger")
                includeGroup("com.google.dagger.hilt.android")
            }
        }
        mavenCentral()
        google()
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
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // App Hilt/Compiler coordinates must use the same fallback as the
        // plugin. Keep this alternative limited to the Dagger group.
        maven(url = "https://repo1.maven.org/maven2") {
            content {
                includeGroup("com.google.dagger")
            }
        }
        // Fallback for Hilt runtime and annotation processor dependencies.
        maven(url = "https://maven-central.storage-download.googleapis.com/maven2") {
            content { includeGroup("com.google.dagger") }
        }
        mavenCentral()
    }
}

rootProject.name = "EBookReader"
include(":app")
include(":benchmark")
include(":third_party:subsampling-scale-image-view")

include(":third_party:backdrop")
include(":third_party:word-converters")
