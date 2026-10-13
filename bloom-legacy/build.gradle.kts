plugins {
    id("bloom.java8-library")
    id("bloom.publishing")
}

description = "Bloom adapter for legacy Bukkit/Spigot 1.7.10 - 1.8.9 (Java 8). NMS-only features are reflection-probed."

dependencies {
    implementation(project(":bloom-bukkit"))
    implementation(project(":bloom-common"))
    compileOnly(libs.spigot.api.legacy)

    testImplementation(libs.spigot.api.legacy)
    testImplementation(platform(libs.junit6.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

// 1.7.10 note: classes named *V18 intentionally reference 1.8-only API (ItemFlag, ItemMeta.Spigot, sendTitle) and
// are only loaded after a successful runtime probe. Verifying the rest against a 1.7.10 API jar is done with
// bloom-doctor's linkage check (see docs/architecture/COMPATIBILITY.md), not by compiling.
