plugins {
    id("bloom.plugin-jar")
}

description = "Bloom example: lifecycle, messages, scheduler, events and cooldowns."

dependencies {
    implementation(project(":bloom-bukkit"))
    runtimeOnly(project(":bloom-legacy"))
    runtimeOnly(project(":bloom-modern"))
}
