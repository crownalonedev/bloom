# Bloom Progress

_Last updated: 2026-10-13 (session 2)._ Update this file at the end of every work session.

## Current phase
Phase 6 — Website (Phases 1–5 implemented and verified on real servers).

## Completed (implemented + tested)
- **Phase 1 foundation**: Gradle 9.8.1 wrapper, build-logic convention plugins, version catalog, toolchains
  (JDK 21 compiler, JDK 8 test runtime, JDK 25 for 26.x verification), Spotless, publishing convention (local staging
  repo only), architecture docs + ADRs + compatibility matrix.
- **bloom-api**: platform model, SupportPolicy, capabilities, services, lifecycle, scheduler API, text, Cooldowns,
  Durations, ExpiringCache. Tests on Java 21 and Java 8.
- **bloom-common**: service registry, TrackingScheduler, MiniMessageLite, LegacyText, TextTemplates. Tests on 21 and 8.
- **bloom-bukkit**: `Bloom` entry point (enable/get/shutdown, banner, relocation warning), PlatformDetector,
  AdapterLoader (relocation-safe), adapter SPI (Text/Player/Item platforms), BukkitSchedulerBackend, Messenger
  (chat/action bar/title/boss bar + UnsupportedFeaturePolicy), Events (subscribe/listen/limit/filter), BloomOptions.
- **bloom-modern**: Paper adapter (Adventure, MiniMessage, boss bars, PDC, component titles, getHolder(false)).
  Compile-verified against paper-api 1.21.1, 1.21.11 and 26.3 (JDK 25).
- **bloom-legacy**: 1.7.10–1.8.9 adapter with reflection probes (titles, action bar NMS, NBT, item flags,
  unbreakable), 1.7.10-safe online players, 32-char title truncation. Verified on real PaperSpigot 1.7.10 and 1.8.8 servers (2026-10-13).
- **bloom-items**: Items service, ItemBuilder, ItemFlagKey, MaterialMappings (>200 built-in modern↔legacy rows),
  persistent data + custom ids, CustomItem registry, serialization.
- **bloom-gui**: Menus service, Menu builder (layouts, templates, border/fill, pagination), Button (dynamic,
  permission, visibility), ClickContext (deferred navigation), MenuView/MenuState/StateKey, ConfirmMenu,
  MenuTemplates. Duplication-safe listeners, debounce, legacy ghost-item resync, cleanup on close/quit/shutdown.
- **bloom-config**: YamlConfig (defaults copied verbatim, typed getters with file/line errors, comment-preserving
  save/migrations, timestamped backups, atomic writes, safe reload, opt-in addMissingDefaults), ConfigSection,
  POJO mapping (kebab-case, @ConfigKey), TypeSerializer, ConfigValidation, migrations. 15 tests on Java 21 + 8.
- **bloom-storage**: Database (SQLite/MySQL/MariaDB via HikariCP 4.0.3, bounded executor, fail-fast queue,
  transactions, migrations with history table, shutdown that fails never-run work), BlockingDatabase (refuses main
  thread), Repository/SqlRepository, Dialect.upsert. 8 SQLite tests on Java 21 + 8. MySQL/MariaDB tests written
  and **verified in CI** against MySQL 8.4 + MariaDB 11 (`.github/workflows/network-databases.yml`, 2026-10-13).
- **bloom-testing**: ManualScheduler, TestClock. 4 tests on Java 21 + 8.
- **Examples** (`bloom-examples/*`): basic, menus, items, config, storage, cross-version — shaded + relocated jars.
- **bloom-selftest** + `scripts/realserver/run.sh`: real-server harness (downloads official Paper builds, verifies
  SHA-256, picks JDK 8/21/25, runs self-test, collects results.json). Refuses to run without
  `BLOOM_ACCEPT_MINECRAFT_EULA=true`.
- **bloom-integration-tests**: MockBukkit (Paper 1.21.11 API): BloomCoreIT (9), ItemsIT (6), MenusIT (15),
  ExamplePluginsIT (7: loads every shipped example jar + the self-test plugin). Fails on any skipped test.

## Partially implemented

## Remaining (in order)
1. Website + documentation platform (Phases 6–7).
2. AI integration: AGENTS.md, CLAUDE.md, skill, llms.txt, API manifest (Phase 8).
3. bloom-doctor (jar checks + linkage verification), CI workflows, release prep (Phases 9–10).

## Real-server verification (2026-10-13, Windows, scripts/realserver/run.bat)
- PASS on PaperSpigot 1.7.10, PaperSpigot 1.8.8, Paper 1.21.11, Paper 26.3 (all self-test checks).
- Bugs found and fixed: SQLite on the sqlite-jdbc 3.7.2 driver bundled with 1.7.10/1.8 (journal_mode property,
  missing Connection.isValid); BloomData NBT dropped on 1.7.10 (asBukkitCopy); harness issues on Windows (CRLF from
  Python, 1.7.10 EULA flag crash, /dev/null console flood).
- CI: MySQL 8.4 + MariaDB 11 storage tests run in GitHub Actions (.github/workflows/network-databases.yml).

## Blockers
- No official Bukkit 1.7.10 API artifact exists; 1.7.10 is verified only through real-server runs.

## Tests performed (all via `./gradlew build`)
- bloom-api 22 tests, bloom-common 20 tests (each on Java 21 and Java 8), bloom-bukkit 5, bloom-legacy 5,
  bloom-items 7, integration 30 (MockBukkit 4.116.3 / Paper API 1.21.11). All pass, 0 skipped.
- bloom-modern compile verification against Paper 1.21.1 and 26.3.

## Known limitations
- 1.7.10: CraftItemStack.setItemMeta rebuilds the NBT tag, so changing meta after Bloom stores item data probably
  drops it (no unhandledTags before 1.8). Unverified; not covered by the self-test.
- MiniMessage on legacy servers is a documented subset (MiniMessageLite).
- Title timings are ignored on 1.8 (client defaults); no titles/action bars on 1.7.10.
- Item data written on legacy (NBT) and modern (PDC) servers is not cross-readable.
- Folia unsupported; 1.9–1.21.0 unsupported.

## Lessons recorded (important for future work)
- MockBukkit turns UnimplementedOperationException into *skipped* tests; integration tests fail on skips.
- MockBukkit lacks `Inventory#getHolder(boolean)`; the modern adapter uses `getHolder()` only on MockBukkit.
- Shadow relocation patterns must end with '.' (prefix matching would relocate `...bloomexamples`).
- Java 8 plugins bundling bloom-modern need `java { disableAutoTargetJvm() }` and runtimeOnly adapter deps.

## Next step
Build the website (website/): React + Vite + Tailwind + MDX + Shiki, static prerender, Cloudflare Workers assets.
