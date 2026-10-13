// Conventions for Bukkit plugins that embed Bloom (examples, self-test plugin).
//
// - Compiled as Java 8 bytecode against spigot-api 1.8.8, so one jar runs on 1.7.10 through 26.x.
// - Bloom and its third-party libraries are shaded and RELOCATED. Plugin code must therefore live outside the
//   io.github.crownalonedev.bloom package, or it would be relocated as well.
// - Never use shadowJar.minimize(): Bloom loads its adapters reflectively and minimize() would strip them.

plugins {
    java
    id("com.gradleup.shadow")
    id("com.diffplug.spotless")
}

val libs = the<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    // The plugin is Java 8 bytecode but bundles bloom-modern (Java 21 bytecode). Gradle would reject that
    // dependency by default; it is safe because Bloom only loads the modern adapter on Java 21+ servers.
    disableAutoTargetJvm()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options", "-Xlint:-deprecation", "-Xlint:-processing"))
}

dependencies {
    "compileOnly"(libs.findLibrary("spigot-api-legacy").get())
}

tasks.named<ProcessResources>("processResources") {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

val libsPackage = "io.github.crownalonedev.bloomexamples." +
    project.name.removePrefix("example-").removePrefix("bloom-").replace("-", "") + ".libs"

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier = ""
    // Patterns end with '.' so they match whole package names only: "io.github.crownalonedev.bloom" alone would
    // also match "io.github.crownalonedev.bloomexamples" and relocate the plugin's own classes.
    relocate("io.github.crownalonedev.bloom.", "$libsPackage.bloom.")
    relocate("org.yaml.snakeyaml.", "$libsPackage.snakeyaml.")
    relocate("com.zaxxer.hikari.", "$libsPackage.hikari.")
    relocate("org.slf4j.", "$libsPackage.slf4j.")
    exclude("META-INF/maven/**", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
}

tasks.named<Jar>("jar") {
    archiveClassifier = "plain"
}

tasks.named("assemble") {
    dependsOn("shadowJar")
}

spotless {
    // Match .gitattributes (eol=lf); the platform default writes CRLF on Windows.
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    java {
        target("src/*/java/**/*.java")
        palantirJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
