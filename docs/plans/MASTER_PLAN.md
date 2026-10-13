# Bloom Master Plan

This is the roadmap. Live status lives in [`PROGRESS.md`](PROGRESS.md); design in
[`../architecture/ARCHITECTURE.md`](../architecture/ARCHITECTURE.md).

## Phase 1 — Repository foundation
- Gradle 9.8.1 wrapper, Kotlin DSL, `build-logic` convention plugins (`bloom.java8-library`,
  `bloom.java21-library`, `bloom.java8-runtime-tests`, `bloom.publishing`), version catalog.
- Toolchains: JDK 21 compiler with `--release 8|21`; JDK 8 test runtime; JDK 25 for 26.x verification.
- Spotless + Palantir Java Format; `-Xlint`; Javadoc doclint.
- Architecture docs, ADRs, compatibility matrix, this plan, progress tracker.

## Phase 2 — Core
- `bloom-api`: platform model, `SupportPolicy`, capabilities, services, lifecycle, scheduler, text, utilities.
- `bloom-common`: service registry, tracking scheduler, MiniMessage subset converter, placeholder templates.
- `bloom-bukkit`: `Bloom` entry point, platform detection, adapter SPI + loader, Bukkit scheduler backend,
  messenger, event subscriptions, startup banner, relocation check.
- `bloom-testing`: manual scheduler for consumer tests.

## Phase 3 — Adapters
- `bloom-legacy` (1.7.10–1.8.9): reflection-probed online players, titles, action bar, NBT, unbreakable.
- `bloom-modern` (Paper 1.21.1–26.3.x): Adventure text, MiniMessage, boss bars, PDC, component titles.
- Compile verification of `bloom-modern` against paper-api 1.21.1 and 26.3 (JDK 25).
- MockBukkit integration suite (`bloom-integration-tests`).
- Real-server harness (`scripts/realserver`): download official Paper builds, boot headless with a self-test
  plugin, collect results. Requires the developer to accept the Minecraft EULA locally.

## Phase 4 — Developer APIs (one group at a time, each with tests)
1. Items: builder, material mapping, item flags, identity/persistent data, serialization, custom item registry.
2. GUI: menus, buttons, layouts, templates, pagination, back navigation, confirmation, refresh, per-player state,
   duplication-safe event handling, shutdown cleanup.
3. Config: YAML load/save (atomic), defaults, typed getters with line-numbered errors, validation, object mapping,
   custom serializers, versioned migrations with backups, reload.
4. Messaging enhancements: titles/action bars/boss bars with fallback policy.
5. Storage: SQLite/MySQL/MariaDB, async API on a bounded executor, transactions, migrations, repositories.
6. Utilities: cooldowns, durations, caches (done in Phase 2), event subscriptions.

## Phase 5 — Examples
`bloom-examples/example-{basic,menus,items,config,storage,cross-version}`: real plugins, shaded + relocated,
compiled in CI, used as the source for website snippets.

## Phase 6 — Website
`website/`: React 19 + TypeScript + Vite + Tailwind 4 + Motion + Lucide; sakura design system (dark first + light);
homepage sections per brief; petals effect respecting reduced motion and hidden tabs.

## Phase 7 — Documentation platform
MDX docs (all categories in brief), sidebar/TOC/breadcrumbs/prev-next, build-time Shiki, copy buttons, tabs,
static search index with Ctrl/Cmd+K, API reference generated from Javadoc/source metadata, downloads/changelog from
GitHub Releases metadata + `CHANGELOG.md`, examples page linking to sources.

## Phase 8 — AI integration
`AGENTS.md`, `CLAUDE.md`, `.claude/skills/bloom/` skill, generated `llms.txt`/`llms-full.txt`, generated
`api-manifest.json`.

## Phase 9 — Quality
`bloom-doctor` (jar checks + Bukkit linkage verification), accessibility checks, security review, performance
review, CI workflows (Java build, Java 8 tests, website lint/type/build, docs validation, dependency audit).

## Phase 10 — Release preparation
Versioning policy, release workflow (manual, unpublished), release notes, publishing guide (GitHub Packages /
Maven Central), Cloudflare deployment guide. **No publishing or deployment without owner approval.**
