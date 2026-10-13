// Code shown on the website. Compiled (never shipped) so every Java sample in the docs is real, current API usage.
// Regions between "// @snippet:<id>" and "// @end" are extracted by website/scripts/build-content.mjs.
plugins {
    id("bloom.plugin-jar")
}

dependencies {
    implementation(project(":bloom-gui"))
    implementation(project(":bloom-config"))
    implementation(project(":bloom-storage"))
    implementation(project(":bloom-testing"))
}

dependencies {
    testImplementation(platform(libs.junit6.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
