// bloom-doctor: checks built plugin jars that embed Bloom (relocation, adapters, Java level, plugin.yml) and verifies
// that every Bukkit/Paper reference links against each target server API. Runs as a CLI (`java -jar`) and in
// Bloom's own build against the example plugins (`verifyExamplePlugins`, part of `check`).
plugins {
    id("bloom.java8-runtime-tests")
    id("bloom.publishing")
}
// On the classpath through build-logic; the plugins {} block would demand a version.
apply(plugin = "com.gradleup.shadow")

description = "Bloom Doctor: checks plugin jars for relocation, adapter, Java level and Bukkit API linkage problems."

dependencies {
    implementation(libs.asm)
}

tasks.named<Jar>("jar") {
    manifest { attributes("Main-Class" to "io.github.crownalonedev.bloom.doctor.Doctor") }
}

// Standalone CLI: bloom-doctor-<version>-all.jar with ASM relocated.
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier = "all"
    relocate("org.objectweb.asm.", "io.github.crownalonedev.bloom.doctor.libs.asm.")
    manifest { attributes("Main-Class" to "io.github.crownalonedev.bloom.doctor.Doctor") }
}

// ---- Bloom's own linkage verification -------------------------------------------------------------------------
// Runs the doctor on every shipped example plugin and the self-test plugin against the oldest legacy API Bloom can
// obtain (spigot-api 1.8.8) and the modern APIs it supports (paper-api 1.21.11 and 26.3).
fun apiClasspath(name: String, dependency: Provider<MinimalExternalModuleDependency>) =
    configurations.create(name) {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = true
        attributes {
            // paper-api 26.x is Java 25 bytecode; only its class files are read, never loaded.
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
        }
    }.also { dependencies.add(name, dependency) }

val legacyApi = apiClasspath("doctorSpigot1_8_8", libs.spigot.api.legacy)
val modernApi = apiClasspath("doctorPaper1_21_11", libs.paper.api.modern.asProvider())
val nextApi = apiClasspath("doctorPaper26_3", libs.paper.api.next)

val examplePlugins = listOf("basic", "menus", "items", "config", "storage", "cross-version")
    .map { ":bloom-examples:example-$it" } + ":bloom-selftest"

examplePlugins.forEach { evaluationDependsOn(it) }
val pluginJars = files(examplePlugins.map { path ->
    project(path).tasks.named("shadowJar").map { it.outputs.files }
})

val verifyExamplePlugins = tasks.register<JavaExec>("verifyExamplePlugins") {
    description = "Runs bloom-doctor on the example and self-test plugin jars against spigot-api 1.8.8 and paper-api."
    group = "verification"
    mainClass = "io.github.crownalonedev.bloom.doctor.Doctor"
    classpath = sourceSets["main"].runtimeClasspath
    inputs.files(pluginJars, legacyApi, modernApi, nextApi)
    val marker = layout.buildDirectory.file("doctor/verified.txt")
    outputs.file(marker)
    val jars: FileCollection = pluginJars
    val legacy: FileCollection = legacyApi
    val modern: FileCollection = modernApi
    val next: FileCollection = nextApi
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "--legacy-api", "spigot-api-1.8.8=" + legacy.files.joinToString(","),
            "--modern-api", "paper-api-1.21.11=" + modern.files.joinToString(","),
            "--modern-api", "paper-api-26.3=" + next.files.joinToString(","),
        ) + jars.files.map { it.absolutePath }.sorted()
    })
    doLast { marker.get().asFile.apply { parentFile.mkdirs(); writeText("ok\n") } }
}

tasks.named("check") {
    dependsOn(verifyExamplePlugins)
}

// RealApiTest checks known hazards against the real API jars.
tasks.withType<Test>().configureEach {
    val legacy: FileCollection = legacyApi
    val modern: FileCollection = modernApi
    inputs.files(legacy, modern)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "-Ddoctor.api.legacy=" + legacy.files.joinToString(","),
            "-Ddoctor.api.modern=" + modern.files.joinToString(","),
        )
    })
}
