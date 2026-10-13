# Working on Bloom

Instructions for coding agents (and people) changing Bloom itself. To *use* Bloom in a plugin, see
`.claude/skills/bloom/SKILL.md` and the website docs.

## Layout

| Path | Contents |
|---|---|
| `bloom-api` | Platform-neutral contracts (Java 8). No Bukkit imports. |
| `bloom-common` | Shared implementation (Java 8): service registry, scheduler tracking, text conversion. Not public API. |
| `bloom-bukkit` | `Bloom` entry point, detection, adapter SPI/loader, messenger, events (Java 8, compiled against spigot-api 1.8.8). |
| `bloom-legacy` | Adapter for 1.7.10 – 1.8.9. Version-specific members are reached by reflection, probed once at startup. |
| `bloom-modern` | Adapter for Paper 1.21.1 – 26.3.x (Java 21, Adventure). Compile-verified against paper-api 1.21.1, 1.21.11, 26.3. |
| `bloom-items`, `bloom-gui`, `bloom-config`, `bloom-storage`, `bloom-testing` | Feature modules (Java 8). |
| `bloom-examples/*` | Example plugins (shaded + relocated) and `docs-snippets`, the compiled source of website code samples. |
| `bloom-integration-tests` | MockBukkit (Paper 1.21.11 API) suites, including loading every example jar. |
| `bloom-selftest`, `scripts/realserver` | Self-test plugin and the real-server harness. |
| `bloom-apidoc` | Build-time doclet for `./gradlew apiManifest`. |
| `bloom-doctor` | CLI that checks built plugin jars (relocation, Java level, Bukkit API linkage). |
| `website/` | Documentation site (React, Vite, MDX). Content in `website/src/content/docs`. |
| `docs/` | Architecture, ADRs (`DECISIONS.md`), compatibility matrix, master plan, `PROGRESS.md`, guides (versioning, releasing, publishing, deployment), reviews. |

## Commands

```bash
./gradlew build                 # everything: Spotless, Javadoc, tests on Java 21 + Java 8, MockBukkit ITs
./gradlew spotlessApply         # format (Palantir Java Format) before committing
./gradlew apiManifest           # build/api/api-manifest.json
bash scripts/verify-template.sh # publish to Maven Local and build the standalone template
cd website && npm ci && npm run check && npm run build
```

## Rules

- **Java level.** Every module except `bloom-modern` and build tools is Java 8 bytecode with `options.release = 8`
  (restricts the JDK API too). No `var`, `List.of`, records, etc. Tests also run on a real Java 8 runtime.
- **Bukkit API level.** `bloom-bukkit` and feature modules compile against spigot-api 1.8.8 and must only call members
  that exist on every supported server. Anything else goes through an adapter (`TextPlatform`, `PlayerPlatform`,
  `ItemPlatform`) or a reflection probe in `bloom-legacy`. A feature that may be missing gets a `Capability`, and
  callers degrade through `UnsupportedFeaturePolicy` instead of throwing.
- **Legacy reflection.** Resolve members once at startup; enable a capability only if every member resolved. Note that
  1.7.10 differs from 1.8 in ways unit tests cannot see (e.g. no `CraftMetaItem.unhandledTags`, sqlite-jdbc 3.7.2).
- **Public API.** Everything outside `.internal` and `.common` packages is public: document it (Javadoc is checked with
  doclint) and treat changes as API changes (`CHANGELOG.md`).
- **Docs never drift.** Website code samples come from compiled sources marked `// @snippet:id` … `// @end`; change the
  Java, not the MDX. Keep `docs/compatibility.json`, `docs/architecture/COMPATIBILITY.md` and `SupportPolicy` in sync.
- **Tests.** Add unit tests with the change. MockBukkit turns unimplemented operations into *skipped* tests, and the
  integration suite fails on skips: implement or avoid the operation rather than ignoring it.
- **Real servers.** `scripts/realserver/run.sh` (or `run.bat`) downloads official server jars and runs them, which means
  accepting the Minecraft EULA. Only the project owner runs it; ask them, do not set `BLOOM_ACCEPT_MINECRAFT_EULA`
  yourself.
- **No publishing or deployment** (Maven repositories, GitHub releases, Cloudflare) without the owner's explicit
  approval. `publishAllPublicationsToStagingRepository` only writes `build/staging-repo`.
- Update `docs/plans/PROGRESS.md` at the end of a work session.
