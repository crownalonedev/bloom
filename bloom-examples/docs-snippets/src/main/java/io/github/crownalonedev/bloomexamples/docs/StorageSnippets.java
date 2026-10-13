package io.github.crownalonedev.bloomexamples.docs;

import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.storage.Database;
import io.github.crownalonedev.bloom.storage.Migration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Storage snippets used in the documentation. */
final class StorageSnippets {

    private StorageSnippets() {}

    static Database open(JavaPlugin plugin, Bloom bloom) {
        // @snippet:storage.open
        Database db = Database.builder()
                .sqlite(plugin.getDataFolder().toPath().resolve("data.db"))
                .poolName(plugin.getName())
                .migrationTable("myplugin_schema_history")
                .mainThreadCheck(bloom.scheduler()::isPrimaryThread)
                .logger(plugin.getLogger())
                .build();
        bloom.services().register(Database.class, db); // Bloom closes it on shutdown, after queued work
        // @end
        return db;
    }

    static Database mysql(JavaPlugin plugin, Bloom bloom) {
        // @snippet:storage.mysql
        Database db = Database.builder()
                .mysql("localhost", 3306, "minecraft", "bloom", "change-me")
                .poolSize(8)
                .property("useSSL", "true")
                .poolName(plugin.getName())
                .migrationTable("myplugin_schema_history")
                .mainThreadCheck(bloom.scheduler()::isPrimaryThread)
                .build();
        // @end
        return db;
    }

    static void migrate(Database db) {
        // @snippet:storage.migrate
        List<Migration> migrations = Arrays.asList(
                Migration.of(
                        1,
                        "create balances",
                        "CREATE TABLE IF NOT EXISTS balances (uuid VARCHAR(36) PRIMARY KEY, coins BIGINT NOT NULL)"),
                Migration.of(
                        2,
                        "add last seen",
                        tx -> tx.update("ALTER TABLE balances ADD COLUMN last_seen BIGINT NOT NULL DEFAULT 0")));
        db.migrate(migrations).join(); // blocking is fine in onEnable, before the server ticks
        // @end
    }

    static void queries(Bloom bloom, Database db, final Player player) {
        // @snippet:storage.queries
        UUID id = player.getUniqueId();
        db.update("INSERT INTO balances (uuid, coins) VALUES (?, ?)", id, 100L);

        db.queryOne("SELECT coins FROM balances WHERE uuid = ?", rs -> rs.getLong("coins"), id)
                .thenAcceptAsync(
                        coins -> player.sendMessage("Coins: " + coins.orElse(0L)), // main thread
                        bloom.scheduler().mainThreadExecutor())
                .exceptionally(error -> {
                    bloom.plugin().getLogger().warning("Balance lookup failed: " + error.getMessage());
                    return null;
                });
        // @end
    }

    static void transaction(Database db, final UUID from, final UUID to, final long amount) {
        // @snippet:storage.transaction
        db.transaction(tx -> {
            Optional<Long> balance =
                    tx.queryOne("SELECT coins FROM balances WHERE uuid = ?", rs -> rs.getLong(1), from);
            if (!balance.isPresent() || balance.get() < amount) {
                throw new IllegalStateException("Not enough coins"); // rolls back
            }
            tx.update("UPDATE balances SET coins = coins - ? WHERE uuid = ?", amount, from);
            tx.update("UPDATE balances SET coins = coins + ? WHERE uuid = ?", amount, to);
            return balance.get() - amount;
        });
        // @end
    }

    static void blocking(Bloom bloom, final Database db) {
        // @snippet:storage.blocking
        bloom.scheduler().async(() -> {
            // Already off the main thread, so synchronous calls are fine here:
            int rows = db.blocking().update("DELETE FROM balances WHERE coins = 0");
            bloom.plugin().getLogger().info("Removed " + rows + " empty accounts");
        });
        // @end
    }
}
