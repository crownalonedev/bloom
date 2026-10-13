// Public API modules: `apiManifestPart` runs the bloom-apidoc doclet over this module's sources and writes
// build/api-manifest/<module>.json. The root `apiManifest` task merges the parts (see the root build file).

plugins {
    `java-library`
}

val apidoc = configurations.create("apidocDoclet") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    apidoc(project(":bloom-apidoc"))
}

val toolchains = the<JavaToolchainService>()
val mainSources = the<SourceSetContainer>()["main"]

tasks.register<Javadoc>("apiManifestPart") {
    description = "Writes this module's public API as JSON for the API manifest."
    group = "documentation"
    source = mainSources.allJava
    classpath = mainSources.compileClasspath
    javadocTool = toolchains.javadocToolFor { languageVersion = JavaLanguageVersion.of(21) }
    val output = layout.buildDirectory.file("api-manifest/${project.name}.json")
    outputs.file(output)
    destinationDir = layout.buildDirectory.dir("tmp/apiManifestPart").get().asFile
    val opts = options as StandardJavadocDocletOptions
    opts.doclet = "io.github.crownalonedev.bloom.apidoc.ManifestDoclet"
    opts.addStringOption("manifestOut", output.get().asFile.absolutePath)
    opts.addStringOption("moduleName", project.name)
    opts.addStringOption("moduleDescription", project.description ?: "")
    opts.encoding = "UTF-8"
    val docletFiles: FileCollection = apidoc
    inputs.files(docletFiles)
    doFirst {
        (options as StandardJavadocDocletOptions).docletpath = docletFiles.files.toList()
    }
}
