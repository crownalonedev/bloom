# Architecture Decision Records

Short records of decisions that shape Bloom. Each has context, decision and consequences. Newest last.

## ADR-001 — Namespace `io.github.crownalonedev.bloom`

**Context.** No domain is owned for Bloom. `dev.bloom` and similar must not be assumed. Maven Central verifies
`io.github.<user>` namespaces against GitHub accounts; the repository owner's account is `crownalonedev`.
**Decision.** Maven group `io.github.crownalonedev.bloom`; Java packages `io.github.crownalonedev.bloom.<module>`.
**Consequences.** Long but verifiable and conflict-free. Renaming before 1.0 is possible (one `gradle.properties`
change plus a package rename); after 1.0 it would be a breaking change.

## ADR-002 — Two adapter families, shared code compiled against spigot-api 1.8.8

**Context.** Bukkit APIs changed shape across versions (see ARCHITECTURE §3). Compiling shared code against a
modern API would emit references that don't exist on 1.8; compiling per-version copies multiplies maintenance.
**Decision.** Shared Bukkit-facing code is compiled once against spigot-api 1.8.8 and restricted to members whose
binary shape is stable; everything else goes through a `PlatformAdapter` implemented by `bloom-legacy` (Java 8)
and `bloom-modern` (Java 21).
**Consequences.** One shared implementation; discipline required (to be enforced by the bloom-doctor linkage
check). New families (e.g. 1.12, 1.16–1.20) can be added as new adapters without changing the API.

## ADR-003 — Adapters are loaded by derived class name

**Context.** `ServiceLoader` instantiates all providers, which would load Java 21 classes on Java 8 and fail.
Hard-coded string class names break when consumers relocate Bloom.
**Decision.** The loader derives the adapter class name from the runtime package of a Bloom class
(`<runtime package>.platform.legacy.LegacyPlatformAdapter`) and loads only the selected family.
**Consequences.** Relocation-safe without relying on the shading tool rewriting string constants. Adding a family
means adding one mapping entry.

## ADR-004 — Platform API versions

**Context.** Verified 2026-10-12: Paper 26.3 is current (Java 25 bytecode, `paper-api:26.3.build.177-stable`);
1.21.11 requires Java 21; spigot-api 1.8.8 is still hosted by SpigotMC; no official 1.7.10 API artifact is
hosted (history of DMCA takedowns).
**Decision.** Modern adapter compiles with `--release 21` against paper-api 1.21.11 and is compile-verified
against 1.21.1 and 26.3 (the latter with a JDK 25 toolchain). 1.7.10 compile verification is optional and uses a
developer-provided jar (`-Pbloom.bukkit17Jar=...`); Bloom never redistributes server or API jars of uncertain
licensing.
**Consequences.** Modern support spans 1.21.1–26.3.x without a second adapter. 1.7.10 behaviour must be verified on
a real server.

## ADR-005 — Java baselines and library choices

**Context.** Legacy servers run Java 8. Verified bytecode levels: SnakeYAML 2.7 (52), sqlite-jdbc 3.53 (52),
HikariCP 4.0.3 (52), HikariCP 7.x (55 = Java 11), MariaDB Connector/J 3.5 (52). JUnit 6 requires Java 17.
**Decision.** Java 8 modules use `options.release = 8` on a JDK 21 toolchain (restricts JDK API usage, not just
bytecode). Platform-neutral modules also run their tests on a real Java 8 runtime (`testJava8`). Storage compiles
against HikariCP 4.0.3 (API-compatible with newer versions, which modern-only consumers may substitute). JUnit 5.14
for Java 8 suites; JUnit 6 with MockBukkit.
**Consequences.** Java 8 compatibility is verified by execution, not assumed.

## ADR-006 — No SLF4J; java.util.logging

**Context.** Legacy servers do not ship SLF4J; shading it adds weight and binding conflicts. Every Bukkit plugin
already has a JUL `Logger`.
**Decision.** Bloom logs through the plugin's JUL logger. Platform-neutral modules accept a `java.util.logging.Logger`.
**Consequences.** Zero logging dependencies; output appears under the plugin's name on every server.

## ADR-007 — Bloom ships its own SnakeYAML

**Context.** Servers bundle different SnakeYAML versions (1.15 on 1.8.8, 2.x on modern Paper) with incompatible APIs.
**Decision.** `bloom-config` depends on SnakeYAML 2.7, which consumers shade and relocate.
**Consequences.** Identical config behaviour everywhere; consumers must relocate `org.yaml.snakeyaml` (documented,
checked by bloom-doctor).

## ADR-008 — License: MIT (recommended, owner may change before first public release)

**Context.** The owner plans closed-source premium plugins on Bloom, and wants public collaboration.
**Options considered.** MIT (simplest, ecosystem norm for Minecraft libraries); Apache-2.0 (adds explicit patent
grant, NOTICE requirements); MPL-2.0 (file-level copyleft: changes to Bloom files must be shared, closed plugins
allowed); LGPL-3.0 (copyleft; shading/relocation into closed jars complicates relinking obligations); GPL-3.0
(would force plugins using Bloom to be GPL).
**Decision.** MIT. It imposes no obligations on closed-source consumers beyond attribution, which matches the goal
of broad adoption. The copyright holder can always use their own code in any way regardless of license.
**Consequences.** Others may use Bloom in closed-source and commercial work, including forks that don't contribute
back. If reciprocity matters more, MPL-2.0 is the recommended alternative.

## ADR-009 — Tests: MockBukkit for modern, real servers for legacy

**Context.** MockBukkit exists only for modern Paper (`mockbukkit-v1.21`, `v26.x`). No maintained mock exists for
1.8.
**Decision.** Modern adapter, GUI and items logic are tested on MockBukkit, which also proves that the 1.8.8-compiled
shared code links against the 1.21 API. Legacy behaviour is verified with a scripted real-server harness
(`scripts/realserver`) running a self-test plugin on Paper/PaperSpigot jars downloaded from PaperMC's official
API at test time (never committed).
**Consequences.** A version is labelled *tested* only after the harness passes on it.

## ADR-010 — Website: React + Vite static prerender on Cloudflare Workers static assets

**Context.** Requirements: React/TS/Vite/Tailwind/MDX/Shiki/Motion, static generation, Cloudflare hosting, no server.
**Decision.** Vite builds a client bundle and an SSR bundle; a prerender script renders every route to static HTML
at build time; Shiki runs at build time only. Deployed as an assets-only Worker (`wrangler.jsonc`), with
`_headers` for security headers.
**Consequences.** Zero runtime server cost, full SEO, hydration for interactive parts only.
