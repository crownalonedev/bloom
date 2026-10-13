// Maven publishing for Bloom library modules.
// Nothing here publishes to a remote repository: `publishAllPublicationsToStagingRepository` writes a
// local Maven repository (with checksums) into build/staging-repo for inspection and release packaging.
// Remote repositories (GitHub Packages / Maven Central) are documented in docs/guides/PUBLISHING.md and
// must be added deliberately by the project owner.

plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = project.name
                description = provider { project.description ?: "Bloom Framework module ${project.name}" }
                url = "https://github.com/crownalonedev/bloom"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id = "crownalonedev"
                        name = "crownalonedev"
                    }
                }
                scm {
                    url = "https://github.com/crownalonedev/bloom"
                    connection = "scm:git:https://github.com/crownalonedev/bloom.git"
                }
            }
        }
    }
    repositories {
        maven {
            name = "staging"
            url = uri(rootProject.layout.buildDirectory.dir("staging-repo"))
        }
    }
}
