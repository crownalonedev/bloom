package io.github.crownalonedev.bloom.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A {@link Repository} for one table with a single-column primary key. Subclasses describe the mapping; Bloom
 * generates the SQL. Table and column names must be plain identifiers (they are validated to prevent accidental SQL
 * injection through names).
 *
 * <pre>{@code
 * final class StatsRepository extends SqlRepository<UUID, PlayerStats> {
 *     StatsRepository(Database db) {
 *         super(db, "player_stats", "uuid", Arrays.asList("uuid", "kills", "deaths"),
 *                 rs -> new PlayerStats(UUID.fromString(rs.getString("uuid")), rs.getInt("kills"), rs.getInt("deaths")));
 *     }
 *     protected UUID keyOf(PlayerStats s) { return s.uuid(); }
 *     protected Object[] valuesOf(PlayerStats s) { return new Object[] {s.uuid(), s.kills(), s.deaths()}; }
 * }
 * }</pre>
 *
 * @param <K> key type
 * @param <V> value type
 * @since 0.1.0
 */
public abstract class SqlRepository<K, V> implements Repository<K, V> {

    private final Database database;
    private final String table;
    private final String keyColumn;
    private final List<String> columns;
    private final RowMapper<V> mapper;
    private final String selectColumns;

    /**
     * Creates the repository.
     *
     * @param database database
     * @param table table name
     * @param keyColumn primary key column
     * @param columns all columns including the key, in the order of {@link #valuesOf}
     * @param mapper maps a row to a value
     */
    protected SqlRepository(
            Database database, String table, String keyColumn, List<String> columns, RowMapper<V> mapper) {
        this.database = database;
        this.table = Database.identifier(table, "table name");
        this.keyColumn = Database.identifier(keyColumn, "column name");
        List<String> checked = new ArrayList<String>();
        for (String column : columns) {
            checked.add(Database.identifier(column, "column name"));
        }
        if (!checked.contains(keyColumn)) {
            throw new IllegalArgumentException("columns must include the key column '" + keyColumn + "'");
        }
        this.columns = Collections.unmodifiableList(checked);
        this.mapper = mapper;
        this.selectColumns = String.join(", ", checked);
    }

    /**
     * Returns the key of a value.
     *
     * @param value value
     * @return key
     */
    protected abstract K keyOf(V value);

    /**
     * Returns the column values of a value, in the order of the constructor's {@code columns}.
     *
     * @param value value
     * @return column values
     */
    protected abstract Object[] valuesOf(V value);

    /**
     * Returns the database.
     *
     * @return database
     */
    protected Database database() {
        return database;
    }

    /**
     * Returns the table name.
     *
     * @return table
     */
    protected String table() {
        return table;
    }

    @Override
    public CompletableFuture<Optional<V>> find(K key) {
        return database.queryOne(
                "SELECT " + selectColumns + " FROM " + table + " WHERE " + keyColumn + " = ?", mapper, key);
    }

    @Override
    public CompletableFuture<List<V>> findAll() {
        return database.query("SELECT " + selectColumns + " FROM " + table, mapper);
    }

    @Override
    public CompletableFuture<Void> save(V value) {
        Object[] values = valuesOf(value);
        if (values.length != columns.size()) {
            CompletableFuture<Void> failed = new CompletableFuture<Void>();
            failed.completeExceptionally(new IllegalArgumentException(
                    "valuesOf returned " + values.length + " values for " + columns.size() + " columns"));
            return failed;
        }
        String sql = database.dialect().upsert(table, columns, Collections.singletonList(keyColumn));
        return database.update(sql, values).thenApply(count -> null);
    }

    @Override
    public CompletableFuture<Boolean> delete(K key) {
        return database.update("DELETE FROM " + table + " WHERE " + keyColumn + " = ?", key)
                .thenApply(count -> count > 0);
    }
}
