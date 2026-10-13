plugins {
    id("bloom.plugin-jar")
}

description = "Bloom example: menus."

dependencies {
    implementation(project(":bloom-gui"))
    runtimeOnly(project(":bloom-legacy"))
    runtimeOnly(project(":bloom-modern"))
}
