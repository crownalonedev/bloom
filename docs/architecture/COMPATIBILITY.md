# Bloom Compatibility Matrix

Bloom distinguishes four levels. Never read a higher level into a lower one.

| Level | Meaning |
|---|---|
| **Tested (real server)** | The real-server harness booted this exact server version with a Bloom self-test plugin and all checks passed. |
| **Tested (MockBukkit)** | Unit/integration tests ran against MockBukkit for this API line. Covers logic, not real client/server behaviour. |
| **Compile-verified** | Adapter sources compiled against this API version. Says nothing about runtime behaviour. |
| **Accepted at runtime** | `SupportPolicy` lets Bloom start. Always ⊇ compile-verified; may include untested patch releases. |
| **Planned** | No adapter yet. Bloom refuses to start. |

## Server versions

| Minecraft | Server | Java | Adapter | Accepted at runtime | Compile-verified | MockBukkit | Real server |
|---|---|---|---|---|---|---|---|
| 1.7.10 | CraftBukkit / Spigot / PaperSpigot | 8 | legacy | yes | only with user-provided API jar | n/a | not yet |
| 1.8 – 1.8.9 | CraftBukkit / Spigot / PaperSpigot | 8 | legacy | yes | 1.8.8 | n/a | not yet |
| 1.9 – 1.21.0 | any | — | none | **no** (Planned) | — | — | — |
| 1.21.1 – 1.21.11 | Paper (+ forks) | 21 | modern | yes | 1.21.1, 1.21.11 | not yet | not yet |
| 1.21.x | Spigot / CraftBukkit | — | none | **no** (needs Paper API) | — | — | — |
| 26.1 – 26.3.x | Paper (+ forks) | 25 | modern | yes | 26.3 | not yet | not yet |
| newer than 26.3.x | Paper | 25+ | modern | only with `allowNewerUntestedVersions(true)` | — | — | — |
| any | Folia | — | none | **no** (Planned) | — | — | — |

The "not yet" cells are updated by the phase that adds the corresponding test. See `PROGRESS.md`.

## Capabilities by family

| Capability | Legacy 1.7.10 | Legacy 1.8.x | Modern Paper |
|---|---|---|---|
| `ADVENTURE_TEXT`, `MINIMESSAGE`, `HEX_COLORS` | — | — | yes |
| MiniMessage subset → legacy colours | yes | yes | (full MiniMessage) |
| `ACTION_BAR` | — (protocol lacks it) | if NMS probe succeeds | yes |
| `TITLES` | — | if `Player#sendTitle(String,String)` exists (1.8.8 has it) | yes |
| `TITLE_TIMINGS` | — | — (client defaults) | yes |
| `BOSS_BAR` | — | — | yes |
| `COMPONENT_INVENTORY_TITLES` | — (32-char limit) | — (32-char limit) | yes |
| `PERSISTENT_DATA_CONTAINER` | — | — | yes |
| `ITEM_NBT_TAGS` | if NMS probe succeeds | if NMS probe succeeds | — (uses PDC) |
| `ITEM_FLAGS` | — | yes | yes |
| `UNBREAKABLE_ITEMS` | if `ItemMeta.Spigot` probe succeeds | yes (Spigot) | yes |
| `CUSTOM_MODEL_DATA` | — | — | yes |
| `LEGACY_DATA_VALUES` | yes | yes | — |

Probed capabilities are determined at startup and printed in the startup banner.

## Java requirements

| Artifact | Bytecode | Runs on |
|---|---|---|
| bloom-api, bloom-common, bloom-config, bloom-storage, bloom-testing, bloom-bukkit, bloom-items, bloom-gui, bloom-legacy | Java 8 | Java 8+ |
| bloom-modern | Java 21 | Java 21+ (26.x servers require Java 25) |

A plugin that targets both families compiles against spigot-api 1.8.8 with `--release 8` and includes both
adapters. A modern-only plugin may compile against paper-api with Java 21 and include only `bloom-modern`.
