// Build-time only: the Javadoc doclet that writes the API manifest (website API reference, api-manifest.json,
// AI tools). Not published and never shipped in plugins.
plugins {
    id("bloom.java21-library")
}

description = "Bloom build tool: Javadoc doclet that generates the API manifest."

tasks.named<Javadoc>("javadoc") {
    enabled = false
}

dependencies {
    testImplementation(platform(libs.junit6.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}
