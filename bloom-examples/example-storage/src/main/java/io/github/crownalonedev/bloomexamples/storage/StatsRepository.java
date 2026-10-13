package io.github.crownalonedev.bloomexamples.storage;

import io.github.crownalonedev.bloom.storage.Database;
import io.github.crownalonedev.bloom.storage.SqlRepository;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Repository for {@code player_stats}, plus atomic counter updates. */
final class StatsRepository extends SqlRepository<UUID, PlayerStats> {

    StatsRepository(Database database) {
        super(
                database,
                "player_stats",
                "uuid",
                Arrays.asList("uuid", "kills", "deaths"),
                row -> new PlayerStats(
                        UUID.fromString(row.getString("uuid")), row.getInt("kills"), row.getInt("deaths")));
    }

    @Override
    protected UUID keyOf(PlayerStats value) {
        return value.uuid();
    }

    @Override
    protected Object[] valuesOf(PlayerStats value) {
        return new Object[] {value.uuid(), value.kills(), value.deaths()};
    }

    CompletableFuture<Void> addKill(UUID player) {
        return increment(player, "kills");
    }

    CompletableFuture<Void> addDeath(UUID player) {
        return increment(player, "deaths");
    }

    /** Creates the row if needed and increments a counter in one transaction, so concurrent updates are not lost. */
    private CompletableFuture<Void> increment(final UUID player, final String column) {
        return database().inTransaction(tx -> {
            if (tx.update("UPDATE player_stats SET " + column + " = " + column + " + 1 WHERE uuid = ?", player) == 0) {
                tx.update("INSERT INTO player_stats (uuid, " + column + ") VALUES (?, 1)", player);
            }
        });
    }
}
