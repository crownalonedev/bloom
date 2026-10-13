plugins {
    id("bloom.plugin-jar")
}

description = "Bloom example: SQLite player statistics with migrations and a repository."

dependencies {
    implementation(project(":bloom-bukkit"))
    implementation(project(":bloom-storage"))
    runtimeOnly(project(":bloom-legacy"))
    runtimeOnly(project(":bloom-modern"))
}
