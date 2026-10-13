// Maven publishing for Bloom library modules.
// Nothing here publishes to a remote repository: `publishAllPublicationsToStagingRepository` writes a
// local Maven repository (with checksums) into build/staging-repo for inspection and release packaging.
// Remote repositories (GitHub Packages / Maven Central) are documented in docs/guides/PUBLISHING.md and
// must be added deliberately by the project owner.

plugins {
    `java-library`
    `maven-publish`
    signing
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

// Maven Central requires PGP signatures. Signing is opt-in: it only happens when BLOOM_SIGNING_KEY (an ASCII-armored
// private key) is set, e.g. as a secret in the release workflow. Without it, nothing is signed and builds are
// unchanged. See docs/guides/PUBLISHING.md.
// GitHub Actions passes a missing secret as an empty string, so blank means "not configured".
val signingKey = providers.environmentVariable("BLOOM_SIGNING_KEY").filter { it.isNotBlank() }
if (signingKey.isPresent) {
    signing {
        useInMemoryPgpKeys(signingKey.get(), providers.environmentVariable("BLOOM_SIGNING_PASSWORD").orNull)
        sign(publishing.publications["maven"])
    }
}
