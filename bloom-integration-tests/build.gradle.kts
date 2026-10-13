// MockBukkit integration tests (not published). Runs Bloom's shared modules — compiled as Java 8 bytecode against
// spigot-api 1.8.8 — on the Paper 1.21 API provided by MockBukkit, together with the real modern adapter.
// This exercises binary compatibility of the shared code with the modern API in addition to behaviour.

plugins {
    id("bloom.java-base")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

dependencies {
    testImplementation(project(":bloom-bukkit"))
    testImplementation(project(":bloom-items"))
    testImplementation(project(":bloom-gui"))
    testImplementation(project(":bloom-common"))
    testRuntimeOnly(project(":bloom-modern"))

    testImplementation(libs.mockbukkit)
    testRuntimeOnly(libs.sqlite.jdbc) // the storage example uses SQLite, which real servers bundle
    testImplementation(libs.paper.api.modern)
    testImplementation(platform(libs.junit6.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

// Not a library: no javadoc/sources jars needed.
tasks.named("javadoc") { enabled = false }

// The shaded example plugin jars are loaded into MockBukkit to test relocation and adapter loading in the real
// artifacts plugin developers ship.
val exampleProjects = listOf("basic", "menus", "items", "config", "storage", "cross-version")
tasks.named<Test>("test") {
    for (example in exampleProjects) {
        dependsOn(":bloom-examples:example-$example:shadowJar")
    }
    val examplesDir = rootProject.layout.projectDirectory.dir("bloom-examples").asFile.absolutePath
    systemProperty("bloom.examples.dir", examplesDir)
    systemProperty("bloom.version", project.version.toString())
    dependsOn(":bloom-selftest:shadowJar")
    systemProperty("bloom.selftest.jar", rootProject.layout.projectDirectory
        .file("bloom-selftest/build/libs/bloom-selftest-${project.version}.jar").asFile.absolutePath)
    systemProperty("bloom.selftest.shutdown", "false")
}

// MockBukkit reports unimplemented server features as *skipped* tests. A skipped integration test means the
// behaviour was not verified, so treat any skip as a build failure instead of a silent pass.
tasks.named<Test>("test") {
    val failOnSkip = true
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (failOnSkip && suite.parent == null && result.skippedTestCount > 0) {
                throw GradleException(
                    "${result.skippedTestCount} integration test(s) were skipped (often MockBukkit " +
                        "UnimplementedOperationException). See the test report; skipped means unverified.",
                )
            }
        }
    })
}
