pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    // Lets Gradle provision missing JDKs (8/21/25) on CI machines. Local installs are detected automatically.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "bloom"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
            name = "spigotmc"
            content {
                includeGroup("org.spigotmc")
                includeGroup("org.bukkit")
                includeGroup("net.md-5")
            }
        }
    }
}

// Modules are added here as they are implemented (Gradle 9 rejects missing project directories).
// Full planned layout: docs/architecture/ARCHITECTURE.md
include("bloom-api")
include("bloom-common")
include("bloom-config")
include("bloom-storage")
include("bloom-testing")
include("bloom-bukkit")
include("bloom-items")
include("bloom-gui")
include("bloom-legacy")
include("bloom-modern")
include("bloom-integration-tests")
include("bloom-selftest")
include("bloom-apidoc")
include("bloom-examples:example-basic")
include("bloom-examples:example-menus")
include("bloom-examples:example-items")
include("bloom-examples:example-config")
include("bloom-examples:example-storage")
include("bloom-examples:example-cross-version")
include("bloom-examples:docs-snippets")
