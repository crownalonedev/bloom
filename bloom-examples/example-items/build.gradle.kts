plugins {
    id("bloom.plugin-jar")
}

description = "Bloom example: items."

dependencies {
    implementation(project(":bloom-gui"))
    runtimeOnly(project(":bloom-legacy"))
    runtimeOnly(project(":bloom-modern"))
}
