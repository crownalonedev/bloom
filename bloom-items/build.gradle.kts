plugins {
    id("bloom.java8-library")
    id("bloom.publishing")
    id("bloom.api-manifest")
}

description = "Bloom Items: item builder, cross-version materials, persistent item identity and serialization."

dependencies {
    api(project(":bloom-bukkit"))
    implementation(project(":bloom-common"))
    compileOnly(libs.spigot.api.legacy)

    testImplementation(platform(libs.junit6.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}
