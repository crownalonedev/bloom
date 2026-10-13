// Shared conventions for every Bloom Java module.
// Do not apply directly: use bloom.java8-library or bloom.java21-library, which pin the bytecode level.

plugins {
    `java-library`
    id("com.diffplug.spotless")
}

java {
    // Compiler JDK. The *bytecode* level is controlled per module through options.release,
    // which also restricts the JDK API surface to that release (prevents accidental Java 9+ API use).
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all",
            "-Xlint:-options",    // "source value 8 is obsolete" warnings on newer JDKs
            "-Xlint:-processing",
            "-Xlint:-serial",
            "-Xlint:-deprecation", // Bukkit-family code must call APIs deprecated on newer versions
            "-Xlint:-removal",
        ),
    )
}

tasks.withType<Javadoc>().configureEach {
    if (name == "apiManifestPart") {
        return@configureEach // custom doclet (bloom.api-manifest); standard-doclet options do not apply
    }
    val opts = options as StandardJavadocDocletOptions
    opts.encoding = "UTF-8"
    opts.docEncoding = "UTF-8"
    opts.charSet = "UTF-8"
    opts.addStringOption("Xdoclint:all,-missing", "-quiet")
    opts.windowTitle = "Bloom ${project.version} API"
    opts.docTitle = "Bloom ${project.version} — ${project.name}"
    isFailOnError = true
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version.toString(),
            "Implementation-Vendor" to "Bloom Framework",
        )
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
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
