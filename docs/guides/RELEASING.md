# Releasing Bloom

A release is prepared by a manual GitHub Actions workflow that produces a **draft** release. Nothing becomes public
until the owner publishes the draft and, separately, uploads to Maven Central. See [VERSIONING.md](VERSIONING.md)
for how to choose the version number.

## 1. Prepare `main`

1. CI is green on `main` (Java, consumer template, website, network databases).
2. Run the real-server harness on your machine for every supported line and keep the results:

   ```bash
   scripts/realserver/run.bat                         # Windows: 1.7.10, 1.8.8, 1.21.11, 26.3
   BLOOM_ACCEPT_MINECRAFT_EULA=true scripts/realserver/run.sh 1.8.8   # one version, Git Bash / Linux / macOS
   ```

   Update the "Real server" column in `docs/architecture/COMPATIBILITY.md` and `docs/compatibility.json` if anything
   changed.
3. In `CHANGELOG.md`, rename `## [Unreleased]` to `## [X.Y.Z] - YYYY-MM-DD` and add a new empty `## [Unreleased]`
   above it. The release notes are taken from this section.
4. Commit and push.

## 2. Create the draft

GitHub → **Actions** → **Release (draft)** → **Run workflow**, enter `X.Y.Z`. The workflow:

- checks the version format, that the tag does not exist yet and that the changelog section exists;
- builds with `-Pversion=X.Y.Z`: all tests on Java 21 and 8, MockBukkit integration tests, bloom-doctor linkage checks,
  API manifest;
- signs the Maven artifacts if the `BLOOM_SIGNING_KEY` / `BLOOM_SIGNING_PASSWORD` secrets exist;
- creates a draft release `vX.Y.Z` with the Maven Central bundle, the `bloom-doctor` CLI jar, the API manifest, the
  example plugins and `SHA256SUMS`.

## 3. Review and publish

1. Open the draft under **Releases**, check the notes and download the example jars to try them if you like.
2. Upload the Maven Central bundle as described in [PUBLISHING.md](PUBLISHING.md) and wait until the Portal reports
   it as published.
3. Publish the GitHub draft. This creates the `vX.Y.Z` tag.
4. Set `version` in `gradle.properties` to the next `-SNAPSHOT` (e.g. `0.2.0-SNAPSHOT`) and commit.
5. Rebuild and deploy the website ([DEPLOYMENT.md](DEPLOYMENT.md)); its Downloads and Changelog pages read the new
   release and the changelog at build time.
