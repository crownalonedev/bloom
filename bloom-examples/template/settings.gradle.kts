// Standalone template for a plugin that uses Bloom. It is NOT part of the main Bloom build.
// Verify it with scripts/verify-template.sh (publishes Bloom to Maven Local, then builds this project).
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "my-bloom-plugin"
