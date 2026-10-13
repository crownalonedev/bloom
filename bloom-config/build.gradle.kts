plugins {
    id("bloom.java8-runtime-tests")
    id("bloom.publishing")
    id("bloom.api-manifest")
}

description = "Bloom Config: YAML configuration with defaults, validation, object mapping, comment-preserving saves and migrations."

dependencies {
    api(project(":bloom-api"))
    // Shaded and relocated by consumers (servers bundle incompatible SnakeYAML versions; see ADR-007).
    implementation(libs.snakeyaml)
}
