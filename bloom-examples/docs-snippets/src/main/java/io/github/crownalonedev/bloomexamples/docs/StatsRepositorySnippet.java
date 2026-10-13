package io.github.crownalonedev.bloomexamples.docs;

import io.github.crownalonedev.bloom.storage.Database;
import io.github.crownalonedev.bloom.storage.SqlRepository;
import java.util.Arrays;
import java.util.UUID;

// @snippet:storage.repository
final class BalanceRepository extends SqlRepository<UUID, Balance> {

    BalanceRepository(Database db) {
        super(
                db,
                "balances",
                "uuid",
                Arrays.asList("uuid", "coins"),
                rs -> new Balance(UUID.fromString(rs.getString("uuid")), rs.getLong("coins")));
    }

    @Override
    protected UUID keyOf(Balance balance) {
        return balance.uuid;
    }

    @Override
    protected Object[] valuesOf(Balance balance) {
        return new Object[] {balance.uuid, balance.coins}; // same order as the column list
    }
}

final class Balance {
    final UUID uuid;
    final long coins;

    Balance(UUID uuid, long coins) {
        this.uuid = uuid;
        this.coins = coins;
    }
}
// Usage: repository.find(id), repository.save(balance), repository.delete(id), repository.findAll()
// @end
