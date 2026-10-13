// Root project: aggregation only. Module conventions live in build-logic/.
plugins {
    base
}

description = "Bloom Framework — cross-version Minecraft plugin development framework"

// ---- API manifest ---------------------------------------------------------------------------------------------------
// Merges each public module's apiManifestPart into build/api/api-manifest.json, the source of the website's API
// reference, /api-manifest.json and the AI integration docs.
val apiModules = listOf(
    ":bloom-api", ":bloom-bukkit", ":bloom-items", ":bloom-gui", ":bloom-config", ":bloom-storage", ":bloom-testing",
)
apiModules.forEach { evaluationDependsOn(it) }
val apiParts = files(apiModules.map { path ->
    project(path).tasks.named("apiManifestPart").map { it.outputs.files.filter { f -> f.name.endsWith(".json") } }
})
val bloomVersion = version.toString()
tasks.register("apiManifest") {
    description = "Generates build/api/api-manifest.json from the Javadoc of every public module."
    group = "documentation"
    inputs.files(apiParts)
    inputs.property("version", bloomVersion)
    val output = layout.buildDirectory.file("api/api-manifest.json")
    outputs.file(output)
    val parts: FileCollection = apiParts
    val order = apiModules.map { it.removePrefix(":") }
    val version = bloomVersion
    doLast {
        val modules = parts.files.sortedBy { f -> order.indexOf(f.nameWithoutExtension) }
            .joinToString(",") { it.readText().trim() }
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText("{\"bloomVersion\":\"$version\",\"modules\":[$modules]}\n")
        }
    }
}
