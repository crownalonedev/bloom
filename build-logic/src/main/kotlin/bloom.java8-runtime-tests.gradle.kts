// For platform-neutral Java 8 modules: compile tests for Java 8 and additionally run the suite on a real
// Java 8 runtime, so Java 8 compatibility is verified by execution rather than assumed from bytecode level.

plugins {
    id("bloom.java8-library")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    // JUnit 6 requires Java 17, so Java 8 runtime suites use the JUnit 5 line.
    testImplementation(platform(libs.findLibrary("junit5-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-launcher").get())
}

tasks.named<JavaCompile>("compileTestJava") {
    options.release = 8
}

val toolchains = the<JavaToolchainService>()

val testJava8 = tasks.register<Test>("testJava8") {
    description = "Runs the unit tests on a Java 8 runtime."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    javaLauncher = toolchains.launcherFor { languageVersion = JavaLanguageVersion.of(8) }
}

tasks.named("check") {
    dependsOn(testJava8)
}
