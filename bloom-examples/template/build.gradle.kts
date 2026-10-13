plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.example"
version = "1.0.0"

// @snippet:install.gradle-cross
repositories {
    mavenCentral()
    // Bloom is not on a public repository yet: run `./gradlew publishToMavenLocal` in the Bloom repository first.
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // spigot-api
    // spigot-api 1.8.8 depends on bungeecord-chat 1.8-SNAPSHOT, which only PaperMC's repository still serves.
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
    // The plugin is Java 8 bytecode but bundles Bloom's Java 21 modern adapter, which Bloom only loads on Java 21+.
    disableAutoTargetJvm()
}

val bloomVersion = "0.1.0-SNAPSHOT"

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") // compile against the oldest API you support
    implementation("io.github.crownalonedev.bloom:bloom-gui:$bloomVersion") // menus + items + core
    runtimeOnly("io.github.crownalonedev.bloom:bloom-legacy:$bloomVersion") // 1.7.10 - 1.8.9
    runtimeOnly("io.github.crownalonedev.bloom:bloom-modern:$bloomVersion") // Paper 1.21.1 - 26.3.x
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

tasks.shadowJar {
    archiveClassifier = ""
    // Relocate Bloom into your own package so several plugins can embed different Bloom versions.
    // The trailing dots matter: they stop the pattern from matching unrelated packages.
    relocate("io.github.crownalonedev.bloom.", "com.example.myplugin.libs.bloom.")
}
// @end

tasks.processResources {
    filesMatching("plugin.yml") { expand("version" to project.version) }
}
