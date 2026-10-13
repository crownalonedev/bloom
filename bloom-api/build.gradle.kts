plugins {
    id("bloom.java8-runtime-tests")
    id("bloom.publishing")
    id("bloom.api-manifest")
}

description = "Bloom API: stable, platform-neutral contracts (platform model, capabilities, services, scheduling, text, utilities)."

// Generates BloomVersion.java so the running version is available without reading resources
// (resources are not relocated by shading and could collide between plugins).
val generateVersionClass = tasks.register("generateVersionClass") {
    val outputDir = layout.buildDirectory.dir("generated/sources/bloomVersion/java")
    val bloomVersion = project.version.toString()
    inputs.property("version", bloomVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("io/github/crownalonedev/bloom/api/BloomVersion.java").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package io.github.crownalonedev.bloom.api;
            |
            |/**
            | * The Bloom release this code was built as. Generated at build time.
            | *
            | * @since 0.1.0
            | */
            |public final class BloomVersion {
            |
            |    /** The Bloom version string, e.g. {@code "0.1.0"} or {@code "0.2.0-SNAPSHOT"}. */
            |    public static final String VERSION = "$bloomVersion";
            |
            |    private BloomVersion() {}
            |}
            |""".trimMargin(),
        )
    }
}

sourceSets.main {
    java.srcDir(generateVersionClass)
}
