package io.github.crownalonedev.bloomexamples.storage;

import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.storage.Database;
import io.github.crownalonedev.bloom.storage.Migration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bloom Storage example: kill/death statistics in SQLite. All database work runs off the main thread; results are
 * handed back to the main thread before touching players.
 */
public final class StorageExamplePlugin extends JavaPlugin {

    private static final List<Migration> MIGRATIONS = Arrays.asList(Migration.of(
            1,
            "create player_stats",
            "CREATE TABLE IF NOT EXISTS player_stats (uuid VARCHAR(36) PRIMARY KEY, kills INT NOT NULL DEFAULT 0, "
                    + "deaths INT NOT NULL DEFAULT 0)"));

    private Bloom bloom;
    private Database database;
    private StatsRepository stats;

    @Override
    public void onEnable() {
        bloom = Bloom.enable(this);
        database = Database.builder()
                .sqlite(getDataFolder().toPath().resolve("stats.db"))
                .poolName(getName())
                .migrationTable("bloomexample_schema_history")
                .mainThreadCheck(bloom.scheduler()::isPrimaryThread)
                .logger(getLogger())
                .build();
        // Registering the database lets Bloom close it on shutdown (after waiting for queued writes).
        bloom.services().register(Database.class, database);
        // Blocking here is acceptable: the server is still starting and no players are online.
        database.migrate(MIGRATIONS).join();
        stats = new StatsRepository(database);

        bloom.events().subscribe(PlayerDeathEvent.class, event -> {
            Player victim = event.getEntity();
            stats.addDeath(victim.getUniqueId()).exceptionally(this::logFailure);
            Player killer = victim.getKiller();
            if (killer != null) {
                stats.addKill(killer.getUniqueId()).exceptionally(this::logFailure);
            }
        });
        getCommand("stats").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            bloom.messages().send(sender, "&cOnly players have stats.");
            return true;
        }
        final Player player = (Player) sender;
        final UUID id = player.getUniqueId();
        stats.find(id)
                .thenAcceptAsync(
                        found -> {
                            if (!player.isOnline()) {
                                return;
                            }
                            PlayerStats value = found.isPresent() ? found.get() : new PlayerStats(id, 0, 0);
                            bloom.messages()
                                    .send(
                                            player,
                                            "&dKills: &f{kills} &8| &dDeaths: &f{deaths}",
                                            Placeholder.of("kills", value.kills()),
                                            Placeholder.of("deaths", value.deaths()));
                        },
                        bloom.scheduler().mainThreadExecutor())
                .exceptionally(this::logFailure);
        return true;
    }

    private Void logFailure(Throwable error) {
        getLogger().log(Level.SEVERE, "Statistics database operation failed", error);
        return null;
    }

    @Override
    public void onDisable() {
        if (bloom != null) {
            bloom.shutdown(); // closes the registered database after pending writes complete
        }
    }
}
