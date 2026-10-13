---
name: bloom
description: Build Minecraft plugins with the Bloom framework (cross-version 1.7.10 – 26.x). Use when a plugin project depends on io.github.crownalonedev.bloom, or the user asks for Bloom menus, items, config, storage, messages, scheduling or events.
---

# Building plugins with Bloom

Bloom is a library that plugins **shade and relocate**; nothing is installed on the server. One jar can run on
1.7.10 – 1.8.9 (legacy adapter) and Paper 1.21.1 – 26.3.x (modern adapter). 1.9 – 1.21.0, Spigot 1.21 and Folia
are not supported: Bloom refuses to start there with an explanation.

## Before writing code

1. **Find the target servers.** Ask if unknown ("Spigot 1.8.8 and Paper 1.21.11?"). This decides the build:
   - cross-version: compile against `spigot-api:1.8.8`, `options.release = 8`, include `bloom-legacy` **and**
     `bloom-modern` as `runtimeOnly`, and `java { disableAutoTargetJvm() }`;
   - legacy only: same without `bloom-modern` and without `disableAutoTargetJvm()`;
   - modern only: compile against `paper-api`, `options.release = 21`, `runtimeOnly` `bloom-modern` only.
2. **Read the Bloom version** in the build file. Check signatures against the API manifest for that version
   (`/api-manifest.json` on the Bloom website, or `build/api/api-manifest.json` after `./gradlew apiManifest` in the
   Bloom repository). **Only call methods that are in the manifest.** If something is missing, say so; do not invent
   it.
3. **Relocate everything Bloom bundles** in `shadowJar` (trailing dots matter):
   ```kotlin
   relocate("io.github.crownalonedev.bloom.", "com.example.myplugin.libs.bloom.")
   relocate("org.yaml.snakeyaml.", "com.example.myplugin.libs.snakeyaml.") // bloom-config
   relocate("com.zaxxer.hikari.", "com.example.myplugin.libs.hikari.")     // bloom-storage
   relocate("org.slf4j.", "com.example.myplugin.libs.slf4j.")              // bloom-storage
   ```
   Bloom is not on a public Maven repository yet: `./gradlew publishToMavenLocal` in the Bloom repository, then
   `mavenLocal()`. `bloom-examples/template` in the Bloom repository is a complete starting project.

## Core usage

```java
public final class MyPlugin extends JavaPlugin {
    private Bloom bloom;

    @Override public void onEnable() {
        bloom = Bloom.enable(this);                       // detects the server, loads the adapter, prints a banner
        Messenger messages = bloom.messages().withPrefix("&d[MyPlugin] &7");
        bloom.events().subscribe(PlayerJoinEvent.class, e ->
                messages.send(e.getPlayer(), "Hi {name}!", Placeholder.of("name", e.getPlayer().getName())));
        bloom.scheduler().syncRepeating(task -> messages.broadcast("Tip!"),
                Duration.ofMinutes(5), Duration.ofMinutes(5));
    }

    @Override public void onDisable() {
        if (bloom != null) bloom.shutdown();              // cancels tasks, closes menus and services
    }
}
```

- Options: `Bloom.enable(this, BloomOptions.builder().textFormat(...).unsupportedFeaturePolicy(...).build())`.
- Messages accept `&` colour codes, or a MiniMessage subset with `TextFormat.MINIMESSAGE` (full MiniMessage on Paper,
  converted to legacy colours on 1.7/1.8).
- Services hang off the `Bloom` instance: `Items.of(bloom)`, `Menus.of(bloom)`.
- Storage and config are standalone: `Database.builder()...build()`, `YamlConfig.builder(path)...build()`.

## Version-specific features: check, then fall back

| Feature | Legacy 1.7.10 | Legacy 1.8.x | Modern Paper |
|---|---|---|---|
| Titles | no | yes | yes |
| Action bar | no | yes (reflection probe) | yes |
| Boss bar, hex colours, component inventory titles | no | no | yes |
| Item flags | no | yes | yes |
| Persistent item data | NBT (probe) | NBT (probe) | PDC |

`messages.actionBar(...)` and `messages.title(...)` return `false` when skipped; the default policy logs a one-time
warning. Gate optional behaviour with `bloom.has(Capability.TITLES)` and tell the user what happens on servers
without it. Inventory titles longer than 32 characters are cut on legacy servers. Item data written on legacy
servers is not readable on modern ones and vice versa.

## Feature cheat sheet (verify exact signatures in the manifest)

- **Items** (`bloom-items`): `items.builder("DIAMOND_SWORD").name("&bBlade").lore("...").enchant("sharpness", 3)
  .flags(ItemFlagKey.HIDE_ENCHANTS).unbreakable(true).glow().data("key", "value").build()`. Use **modern** material
  names; Bloom maps them on legacy servers. `items.define("id", builder)` returns a `CustomItem` with `create(n)` and
  `matches(stack)`. `items.serialize(stack)` / `items.deserialize(string)`.
- **Menus** (`bloom-gui`): `Menu.builder().title("...").rows(3).layout("#########", "#.......#", "#########")
  .bind('#', border).paginate(view -> buttons).contentSlots('.').previousPage(18, item).nextPage(26, item)
  .build()`, then `menus.open(player, menu)`. Buttons: `Button.of(item, click -> ...)`, `Button.dynamic(view -> item)`,
  `.permission(node)`, `.visibleWhen(view -> ...)`. Use `ConfirmMenu` for yes/no. Clicks are cancelled and
  duplication-safe; never add your own inventory listeners for Bloom menus.
- **Config** (`bloom-config`): `YamlConfig.builder(dataFolder.resolve("config.yml")).defaults(() ->
  getResource("config.yml")).logger(getLogger()).build()`; typed getters (`getString`, `getInt`, `getDuration`,
  `getEnum`, `get(path, Type.class)`, `as(Type.class)` for POJOs with kebab-case keys), `validate()`, `reload()`,
  `save()`, versioned `migration(from, ...)`. Errors include file and line.
- **Storage** (`bloom-storage`): `Database.builder().sqlite(path).build()` (or `mysql`/`mariadb`). Every call returns a
  `CompletableFuture` and runs off the main thread: `query(sql, rs -> ..., params)`, `update(sql, params)`,
  `transaction(tx -> ...)`, `migrate(migrations)`. Switch back to the main thread before touching Bukkit objects
  (`bloom.scheduler().mainThreadExecutor()`). Close the database in `onDisable`.
- **Scheduling**: `sync`, `syncLater`, `syncRepeating`, `async`, `asyncLater`, `asyncRepeating`, `supplySync`,
  `supplyAsync`; durations or ticks. Tasks are cancelled on `bloom.shutdown()`.
- **Events**: `bloom.events().subscribe(Type.class, handler)` or `listen(Type.class)` for filters, priorities and
  limits; subscriptions end on shutdown.
- **Utilities**: `Cooldowns.of(duration)`, `Durations.format/parse`, `ExpiringCache`.
- **Tests**: `bloom-testing` provides `ManualScheduler` and `TestClock`.

## Debugging

Read the startup banner (`Bloom <version> · <server> · adapter: ...`) and the capability list; set
`BloomOptions.builder().debug(true)` to log adapter probes. Common causes: missing adapter module at runtime, missing
relocation (two plugins with different Bloom versions), compiling against a newer API than the oldest target server.
