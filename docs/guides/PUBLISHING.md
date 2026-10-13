# Publishing to a Maven repository

Bloom's build never uploads anything by itself. `publishAllPublicationsToStagingRepository` writes a local Maven
repository to `build/staging-repo`, and `centralBundle` zips it for upload. This guide covers the one-time setup and
the upload. Only the project owner should do this.

## Recommended: Maven Central

Plugin developers can use Maven Central without credentials, which GitHub Packages does not allow.

### One-time setup

1. **Account and namespace.** Sign in at <https://central.sonatype.com> with the `crownalonedev` GitHub account.
   The namespace `io.github.crownalonedev` matches that GitHub user; signing in with GitHub normally verifies it
   automatically, otherwise Central asks you to create a temporary public repository with a given name. Bloom's group
   `io.github.crownalonedev.bloom` lives inside that namespace.
2. **Signing key.** Central requires PGP signatures:

   ```bash
   gpg --quick-gen-key "Bloom releases <you@example.com>" ed25519 sign 2y
   gpg --keyserver keys.openpgp.org --send-keys <KEY-ID>      # Central checks the public key on a keyserver
   gpg --armor --export-secret-keys <KEY-ID> > bloom-signing-key.asc
   ```

   Add two repository secrets (GitHub → Settings → Secrets and variables → Actions): `BLOOM_SIGNING_KEY` (the content of
   `bloom-signing-key.asc`) and `BLOOM_SIGNING_PASSWORD`. Then delete the exported file. Signing is only enabled
   when `BLOOM_SIGNING_KEY` is set, so local builds stay unsigned.

What Central checks is already in place: sources and Javadoc jars, POM name/description/URL, MIT license, developer and
SCM entries (`build-logic/src/main/kotlin/bloom.publishing.gradle.kts`), plus checksums.

### Each release

1. Run the **Release (draft)** workflow ([RELEASING.md](RELEASING.md)). Its draft release contains
   `bloom-X.Y.Z-central-bundle.zip`, signed when the secrets exist.
2. Central Portal → **Publish Component** → upload the zip, deployment name `bloom X.Y.Z`.
3. Wait for validation. If it fails, the Portal names the file and rule (usually a missing signature). Fix, rerun
   the workflow and drop the failed deployment.
4. Press **Publish**. Artifacts appear on Maven Central within about 30 minutes and cannot be changed or deleted
   afterwards, so check the version number first.

To build the bundle locally instead:

```bash
BLOOM_SIGNING_KEY="$(cat bloom-signing-key.asc)" BLOOM_SIGNING_PASSWORD=... \
  ./gradlew -Pversion=X.Y.Z build centralBundle
# -> build/release/bloom-X.Y.Z-central-bundle.zip
```

After the first release, update the website's [installation guide](../../website/src/content/docs/installation.mdx)
and the template (`bloom-examples/template`): replace `mavenLocal()` with `mavenCentral()` and remove the
"not on a public repository yet" notes.

## Alternative: GitHub Packages

Simpler to set up, but **everyone who depends on Bloom then needs a GitHub token**, even for a public repository.
Add a repository to `bloom.publishing.gradle.kts`:

```kotlin
repositories {
    maven {
        name = "github"
        url = uri("https://maven.pkg.github.com/crownalonedev/bloom")
        credentials {
            username = providers.environmentVariable("GITHUB_ACTOR").orNull
            password = providers.environmentVariable("GITHUB_TOKEN").orNull
        }
    }
}
```

and publish with `./gradlew -Pversion=X.Y.Z publishAllPublicationsToGithubRepository` from a workflow with
`permissions: packages: write`.
