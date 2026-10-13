# Bloom

A Java framework for Minecraft plugins that run on **Minecraft 1.7.10 through 26.x** from a single jar. Bloom handles
the version differences (text, titles, items, inventories, NBT vs. persistent data) behind one API, and gives you
the parts most plugins rebuild from scratch: menus, item builders, YAML config, async SQL storage, scheduling,
events and cooldowns.

```java
public final class HelloPlugin extends JavaPlugin {
    private Bloom bloom;

    @Override
    public void onEnable() {
        bloom = Bloom.enable(this);
        Messenger messages = bloom.messages().withPrefix("&d&lBloom &8» &7");
        bloom.events().subscribe(PlayerJoinEvent.class, event -> messages.send(
                event.getPlayer(), "Welcome! This server runs {platform}.",
                Placeholder.of("platform", bloom.platform().describe())));
    }

    @Override
    public void onDisable() {
        bloom.shutdown();
    }
}
```

## Supported servers

| Server | Status |
|---|---|
| CraftBukkit / Spigot / PaperSpigot 1.7.10 | Supported (legacy adapter). No titles or action bars. |
| CraftBukkit / Spigot / PaperSpigot 1.8 – 1.8.9 | Supported (legacy adapter) |
| 1.9 – 1.21.0 | Not supported |
| Paper 1.21.1 – 1.21.11 | Supported (modern adapter, Adventure) |
| Paper 26.1 – 26.3.x | Supported (modern adapter, Java 25 servers) |
| Folia | Not supported |

Verified by booting real PaperSpigot 1.7.10 and 1.8.8 and Paper 1.21.11 and 26.3 servers with a self-test plugin
(`scripts/realserver`). The full matrix is in [docs/architecture/COMPATIBILITY.md](docs/architecture/COMPATIBILITY.md).

## Modules

| Module | What it gives you |
|---|---|
| `bloom-bukkit` | `Bloom` entry point, platform detection, messages/titles/action bars/boss bars, scheduler, events |
| `bloom-items` | Item builder, modern↔legacy material names, persistent item data, custom items, serialization |
| `bloom-gui` | Inventory menus: buttons, layouts, pagination, back navigation, confirmation, per-player state |
| `bloom-config` | YAML config with defaults, typed getters with line-numbered errors, validation, object mapping, migrations |
| `bloom-storage` | Async SQLite/MySQL/MariaDB with pooling, transactions, migrations, repositories |
| `bloom-testing` | Manual scheduler and clock for unit tests |
| `bloom-legacy`, `bloom-modern` | Platform adapters (runtime only); include the ones for the servers you target |

## Installation

Download the latest release from [GitHub Releases](https://github.com/crownalonedev/bloom/releases): unzip
`bloom-<version>-central-bundle.zip` and add the folder as a Maven repository (`maven(uri("path"))`). Bloom is not
on Maven Central yet. For the development version, run `./gradlew publishToMavenLocal` in this repository.

Then shade it into your plugin and **relocate** it into your own package, as described in the
[installation guide](website/src/content/docs/installation.mdx). `bloom-examples/template` is a complete,
standalone plugin project to start from.

## Building

Requires JDK 21 (Gradle provisions JDK 8 and 25 for tests and verification if they are not installed).

```bash
./gradlew build          # all modules, unit tests on Java 21 and Java 8, MockBukkit integration tests
./gradlew apiManifest    # build/api/api-manifest.json (website API reference)
```

Other checks:

- `scripts/realserver/run.sh <version>` (or `run.bat` on Windows) boots an official server with the self-test plugin.
  Running a Minecraft server means accepting the [Minecraft EULA](https://aka.ms/MinecraftEULA); the script asks first.
- `scripts/test-network-databases.sh` runs the storage tests against MySQL and MariaDB in Docker (CI does this too).
- `website/`: `npm ci && npm run build` builds the documentation site and checks its links and accessibility.

## Documentation

- Website: <https://bloom-website.taxablesminecraft.workers.dev>
- Guides: [website/src/content/docs](website/src/content/docs) (published as the Bloom website)
- Check a built plugin jar: `bloom-doctor` ([distribution guide](website/src/content/docs/distribution.mdx))
- Architecture and decisions: [docs/architecture](docs/architecture)
- Versioning, releasing, publishing and deployment: [docs/guides](docs/guides)
- Roadmap and status: [docs/plans](docs/plans)
- Using an AI coding assistant: see [AGENTS.md](AGENTS.md)

## License

[MIT](LICENSE)
