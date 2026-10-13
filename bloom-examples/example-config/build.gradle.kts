plugins {
    id("bloom.plugin-jar")
}

description = "Bloom example: YAML configuration with defaults, validation, object mapping and migrations."

dependencies {
    implementation(project(":bloom-bukkit"))
    implementation(project(":bloom-config"))
    runtimeOnly(project(":bloom-legacy"))
    runtimeOnly(project(":bloom-modern"))
}
