plugins {
    id("bloom.plugin-jar")
}

description = "Bloom example: one jar for 1.7.10 to 26.x using capability checks and fallbacks."

dependencies {
    implementation(project(":bloom-gui"))
    runtimeOnly(project(":bloom-legacy"))
    runtimeOnly(project(":bloom-modern"))
}
