# Versioning policy

Bloom uses [semantic versioning](https://semver.org/): `MAJOR.MINOR.PATCH`, with `-rc.N` (or `-alpha.N`, `-beta.N`)
for pre-releases and `-SNAPSHOT` for development builds that are never released.

## What counts as public API

Everything a plugin can compile against, **except** packages named `internal` or `common`:

- public and protected types and members in `bloom-api`, `bloom-bukkit`, `bloom-items`, `bloom-gui`, `bloom-config`,
  `bloom-storage` and `bloom-testing` (exactly what `./gradlew apiManifest` lists);
- documented behaviour: capability gating, fallback policies, thread rules (e.g. storage refusing the main thread),
  the YAML and item-data formats Bloom writes;
- the set of supported server versions in `docs/compatibility.json`.

Not public: `bloom-common`, the adapters (`bloom-legacy`, `bloom-modern`; add them as `runtimeOnly`), `.internal`
packages, log message wording, and the `bloom-doctor` report format (its exit codes are public).

## Before 1.0

`0.x` releases may change public API in a **minor** version (`0.1` → `0.2`). Patch releases (`0.1.0` → `0.1.1`) are
compatible fixes only. Every breaking change is listed under **Changed** or **Removed** in `CHANGELOG.md` with a
migration note.

## From 1.0

- **Patch** (`1.2.3` → `1.2.4`): bug fixes; no API or format changes.
- **Minor** (`1.2` → `1.3`): new API, new supported server versions, new capabilities. Existing code keeps compiling
  and behaving the same. Deprecations are allowed.
- **Major** (`1.x` → `2.0`): removals and incompatible changes. A deprecated member stays for at least one minor
  release before it is removed, with `@Deprecated` and a Javadoc `@deprecated` note naming the replacement.

## Server support changes

- Adding a server version (e.g. a new Paper release) is a minor change and requires: the API compile check, MockBukkit
  or real-server verification, and updates to `SupportPolicy`, `docs/compatibility.json` and
  `docs/architecture/COMPATIBILITY.md`.
- Dropping a server version is a major change after 1.0 (a minor change before 1.0), announced in the previous
  release's notes.
- Patch releases of an already supported line (e.g. Paper 26.3.1) are accepted at runtime without a Bloom release.

## Data compatibility

Config migrations, the storage migration table and item data keys written by Bloom stay readable by later versions
within the same major version. A format change ships with an automatic migration.
