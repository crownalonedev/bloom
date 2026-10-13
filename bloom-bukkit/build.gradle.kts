plugins {
    id("bloom.java8-library")
    id("bloom.publishing")
    id("bloom.api-manifest")
}

description = "Bloom for Bukkit-family servers: Bloom entry point, platform detection, adapter SPI, scheduler, messaging and events."

dependencies {
    api(project(":bloom-api"))
    implementation(project(":bloom-common"))
    // Lowest common Bukkit API. Shared code may only use members whose binary shape is identical on every
    // supported server; see docs/architecture/ARCHITECTURE.md section 3.
    compileOnly(libs.spigot.api.legacy)

    testImplementation(platform(libs.junit6.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

dependencies {
    testImplementation(libs.spigot.api.legacy)
}
