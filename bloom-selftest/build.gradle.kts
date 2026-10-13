plugins {
    id("bloom.plugin-jar")
}

description = "Bloom self-test plugin for the real-server harness (scripts/realserver). Not published."

dependencies {
    implementation(project(":bloom-gui"))
    implementation(project(":bloom-config"))
    implementation(project(":bloom-storage"))
    runtimeOnly(project(":bloom-legacy"))
    runtimeOnly(project(":bloom-modern"))
}
