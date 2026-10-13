plugins {
    id("bloom.java21-library")
    id("bloom.publishing")
}

description = "Bloom adapter for modern Paper (1.21.1 - 26.3.x): Adventure text, MiniMessage, boss bars, PersistentDataContainer."

dependencies {
    implementation(project(":bloom-bukkit"))
    implementation(project(":bloom-common"))
    compileOnly(libs.paper.api.modern)
}

// --- API compile verification -------------------------------------------------------------------------------
// The adapter is built against paper-api 1.21.11. These tasks recompile the same sources against the oldest
// (1.21.1) and newest (26.3) supported APIs so removed or changed API members are caught at build time.
// 26.3's API is Java 25 bytecode, so that check uses a JDK 25 compiler (output still targets Java 21).

val toolchains = the<JavaToolchainService>()

data class ApiTarget(val name: String, val dependency: Provider<MinimalExternalModuleDependency>, val jdk: Int)

val apiTargets = listOf(
    ApiTarget("Paper1_21_1", libs.paper.api.modern.min, 21),
    ApiTarget("Paper26_3", libs.paper.api.next, 25),
)

val verifyTasks = apiTargets.map { target ->
    val config = configurations.create("verify${target.name}Classpath") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    dependencies {
        add(config.name, target.dependency)
        add(config.name, libs.jetbrains.annotations)
        add(config.name, project(":bloom-bukkit"))
        add(config.name, project(":bloom-common"))
    }

    tasks.register<JavaCompile>("verifyCompile${target.name}") {
        description = "Compiles bloom-modern against ${target.name} API to verify source compatibility."
        group = "verification"
        source = sourceSets.main.get().java
        classpath = config
        destinationDirectory = layout.buildDirectory.dir("verify/${target.name}")
        javaCompiler = toolchains.compilerFor { languageVersion = JavaLanguageVersion.of(target.jdk) }
        options.release = 21
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:-options", "-Xlint:-deprecation", "-Xlint:-removal"))
    }
}

tasks.named("check") {
    dependsOn(verifyTasks)
}
