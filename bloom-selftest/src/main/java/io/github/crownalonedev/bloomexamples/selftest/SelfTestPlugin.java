package io.github.crownalonedev.bloomexamples.selftest;

import io.github.crownalonedev.bloom.api.BloomVersion;
import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.api.text.TextFormat;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.bukkit.event.Subscription;
import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import io.github.crownalonedev.bloom.config.YamlConfig;
import io.github.crownalonedev.bloom.gui.Button;
import io.github.crownalonedev.bloom.gui.Menu;
import io.github.crownalonedev.bloom.gui.Menus;
import io.github.crownalonedev.bloom.items.CustomItem;
import io.github.crownalonedev.bloom.items.ItemFlagKey;
import io.github.crownalonedev.bloom.items.Items;
import io.github.crownalonedev.bloom.storage.Database;
import io.github.crownalonedev.bloom.storage.Migration;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Runs Bloom checks on a real server without players, writes {@code results.json} and stops the server. Driven by
 * {@code scripts/realserver/run.sh}. Player-dependent behaviour (clicks, titles shown to a client) is out of scope.
 */
public final class SelfTestPlugin extends JavaPlugin {

    private final List<String[]> results = new ArrayList<String[]>();
    private Bloom bloom;

    private interface Check {
        void run() throws Exception;
    }

    private void check(String name, Check check) {
        try {
            check.run();
            results.add(new String[] {name, "PASS", ""});
            getLogger().info("[BLOOM-SELFTEST] PASS " + name);
        } catch (Throwable t) {
            String reason = t.getClass().getSimpleName() + ": " + t.getMessage();
            results.add(new String[] {name, "FAIL", reason});
            getLogger().severe("[BLOOM-SELFTEST] FAIL " + name + " - " + reason);
            t.printStackTrace();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @Override
    public void onEnable() {
        check("bloom.enable", () -> bloom = Bloom.enable(this));
        if (bloom == null) {
            finish(new ArrayList<CompletableFuture<?>>());
            return;
        }
        final List<CompletableFuture<?>> pending = new ArrayList<CompletableFuture<?>>();
        final AtomicInteger ticks = new AtomicInteger();

        check("platform.detect", () -> {
            require(bloom.platform().minecraftVersion() != null, "no version");
            getLogger()
                    .info("[BLOOM-SELFTEST] platform=" + bloom.platform().describe() + " adapter="
                            + bloom.adapter().description() + " capabilities="
                            + bloom.capabilities().available());
        });
        check(
                "players.online",
                () -> require(bloom.adapter().players().onlinePlayers().isEmpty(), "players?"));
        check("messages.console", () -> {
            bloom.messages()
                    .send(
                            Bukkit.getConsoleSender(),
                            "&d[BLOOM-SELFTEST] legacy &lformat {v}",
                            Placeholder.of("v", BloomVersion.VERSION));
            bloom.messages()
                    .withFormat(TextFormat.MINIMESSAGE)
                    .send(
                            Bukkit.getConsoleSender(),
                            "<light_purple>[BLOOM-SELFTEST] <bold>minimessage</bold> <#F2A7CC>hex");
            String legacy = bloom.messages().withFormat(TextFormat.MINIMESSAGE).legacy("<red>x");
            require(legacy.contains("x"), "legacy render: " + legacy);
        });
        check(
                "scheduler.sync",
                () -> bloom.scheduler()
                        .syncRepeating(
                                task -> {
                                    if (ticks.incrementAndGet() >= 5) {
                                        task.cancel();
                                    }
                                },
                                0,
                                1));
        check(
                "scheduler.async",
                () -> pending.add(bloom.scheduler().supplyAsync(() -> {
                    require(!bloom.scheduler().isPrimaryThread(), "async ran on main thread");
                    return true;
                })));
        check("events.subscribe", () -> {
            final AtomicInteger seen = new AtomicInteger();
            Subscription subscription = bloom.events().subscribe(ServerCommandEvent.class, e -> seen.incrementAndGet());
            Bukkit.getPluginManager().callEvent(new ServerCommandEvent(Bukkit.getConsoleSender(), "bloomselftest"));
            subscription.unsubscribe();
            Bukkit.getPluginManager().callEvent(new ServerCommandEvent(Bukkit.getConsoleSender(), "bloomselftest"));
            require(seen.get() == 1, "seen=" + seen.get());
        });

        final Items items = Items.of(bloom);
        check("items.materials", () -> {
            for (String key : Arrays.asList(
                    "RED_WOOL",
                    "PLAYER_HEAD",
                    "OAK_PLANKS",
                    "CLOCK",
                    "DIAMOND_SWORD",
                    "LIME_STAINED_GLASS_PANE",
                    "ENCHANTED_GOLDEN_APPLE")) {
                require(items.exists(key), "material " + key + " did not resolve");
            }
        });
        check("items.builder", () -> {
            ItemStack sword = items.builder("DIAMOND_SWORD")
                    .name("&dBlade {n}")
                    .placeholders(Placeholder.of("n", 1))
                    .lore("&7line", "plain")
                    .enchant("sharpness", 3)
                    .flags(ItemFlagKey.HIDE_ENCHANTS)
                    .unbreakable(true)
                    .glow()
                    .build();
            require(sword.getItemMeta().hasDisplayName(), "no name");
            require(sword.getEnchantments().size() >= 1, "no enchants");
        });
        check("items.data", () -> {
            require(items.supportsData(), "no item data support (NBT probe failed?)");
            CustomItem gem =
                    items.define("selftest_gem", items.builder("EMERALD").name("&aGem"));
            ItemStack stack = gem.create(2);
            require(gem.matches(stack), "custom id not readable");
            require(gem.matches(stack.clone()), "custom id lost on clone");
            ItemStack tagged = items.setData(stack, "tier", "7");
            require("7".equals(items.getData(tagged, "tier").orElse(null)), "data roundtrip");
            require(!items.getData(items.removeData(tagged, "tier"), "tier").isPresent(), "data remove");
        });
        check("items.serialize", () -> {
            ItemStack original = items.builder("GOLDEN_APPLE")
                    .name("&6Apple")
                    .amount(4)
                    .customId("apple")
                    .build();
            ItemStack restored = items.deserialize(items.serialize(original));
            require(restored.isSimilar(original) && restored.getAmount() == 4, "serialization roundtrip");
        });
        check("gui.inventory", () -> {
            FormattedText title = bloom.messages().render("&d&lA very long inventory title that overflows 32");
            Inventory inventory = bloom.adapter().players().createInventory(null, 27, title);
            require(inventory.getSize() == 27, "size");
            Menu menu = Menu.builder()
                    .title("&dSelf test {page}")
                    .layout("#########", "#   x   #", "#########")
                    .bind(
                            '#',
                            Button.of(items.builder("GRAY_STAINED_GLASS_PANE")
                                    .name(" ")
                                    .build()))
                    .bind('x', Button.of(items.builder("DIAMOND").build()))
                    .build();
            require(menu.size() == 27, "menu size");
            Menus.of(bloom);
        });
        check("config.roundtrip", () -> {
            File file = new File(getDataFolder(), "selftest.yml");
            Files.deleteIfExists(file.toPath());
            YamlConfig config = YamlConfig.builder(file.toPath()).build();
            config.set("section.value", 42);
            config.save();
            YamlConfig reloaded = YamlConfig.builder(file.toPath()).build();
            require(reloaded.getInt("section.value") == 42, "config value");
        });
        check("storage.sqlite", () -> {
            final Database db = Database.builder()
                    .sqlite(new File(getDataFolder(), "selftest.db").toPath())
                    .mainThreadCheck(bloom.scheduler()::isPrimaryThread)
                    .logger(getLogger())
                    .build();
            bloom.services().register(Database.class, db);
            pending.add(db.update("DROP TABLE IF EXISTS selftest")
                    .thenCompose(x -> db.update("DROP TABLE IF EXISTS bloom_schema_history"))
                    .thenCompose(x -> db.migrate(Arrays.asList(
                            Migration.of(1, "create", "CREATE TABLE selftest (id VARCHAR(36) PRIMARY KEY, n INT)"))))
                    .thenCompose(x -> db.update(
                            db.dialect().upsert("selftest", Arrays.asList("id", "n"), Arrays.asList("id")), "a", 1))
                    .thenCompose(x -> db.update(
                            db.dialect().upsert("selftest", Arrays.asList("id", "n"), Arrays.asList("id")), "a", 2))
                    .thenCompose(x -> db.queryOne("SELECT n FROM selftest WHERE id = ?", rs -> rs.getInt(1), "a"))
                    .thenApply(n -> {
                        require(Optional.of(2).equals(n), "upsert result " + n);
                        return n;
                    }));
        });

        bloom.scheduler()
                .syncLater(
                        () -> {
                            check(
                                    "scheduler.ticks",
                                    () -> require(ticks.get() == 5, "repeating task ran " + ticks.get()));
                            finish(pending);
                        },
                        40);
    }

    private void finish(List<CompletableFuture<?>> pending) {
        for (int i = 0; i < pending.size(); i++) {
            final CompletableFuture<?> future = pending.get(i);
            check("async.result." + i, () -> future.get(20, TimeUnit.SECONDS));
        }
        int failed = 0;
        StringBuilder json = new StringBuilder("{\n  \"bloomVersion\": \"")
                .append(BloomVersion.VERSION)
                .append("\",\n");
        json.append("  \"server\": \"").append(escape(Bukkit.getVersion())).append("\",\n");
        if (bloom != null) {
            json.append("  \"platform\": \"")
                    .append(escape(bloom.platform().describe()))
                    .append("\",\n");
            json.append("  \"adapter\": \"")
                    .append(escape(bloom.adapter().description()))
                    .append("\",\n");
            json.append("  \"capabilities\": [");
            boolean first = true;
            for (Capability capability : bloom.capabilities().available()) {
                json.append(first ? "" : ", ")
                        .append('"')
                        .append(capability.name())
                        .append('"');
                first = false;
            }
            json.append("],\n");
        }
        json.append("  \"checks\": [\n");
        for (int i = 0; i < results.size(); i++) {
            String[] r = results.get(i);
            if ("FAIL".equals(r[1])) {
                failed++;
            }
            json.append("    {\"name\": \"")
                    .append(escape(r[0]))
                    .append("\", \"status\": \"")
                    .append(r[1])
                    .append("\", \"detail\": \"")
                    .append(escape(r[2]))
                    .append("\"}")
                    .append(i < results.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ],\n  \"result\": \"")
                .append(failed == 0 ? "PASS" : "FAIL")
                .append("\"\n}\n");
        try {
            getDataFolder().mkdirs();
            Files.write(
                    new File(getDataFolder(), "results.json").toPath(),
                    json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            getLogger().severe("[BLOOM-SELFTEST] could not write results: " + e);
        }
        getLogger()
                .info("[BLOOM-SELFTEST] RESULT " + (failed == 0 ? "PASS" : "FAIL") + " " + (results.size() - failed)
                        + "/" + results.size());
        if (!"false".equals(System.getProperty("bloom.selftest.shutdown"))) {
            Bukkit.getScheduler()
                    .runTaskLater(
                            this,
                            new Runnable() {
                                @Override
                                public void run() {
                                    Bukkit.shutdown();
                                }
                            },
                            20L);
        }
    }

    private static String escape(String text) {
        return text == null
                ? ""
                : text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    @Override
    public void onDisable() {
        if (bloom != null) {
            bloom.shutdown();
        }
    }
}
