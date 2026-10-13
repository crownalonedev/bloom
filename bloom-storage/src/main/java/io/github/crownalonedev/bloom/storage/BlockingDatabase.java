package io.github.crownalonedev.bloom.storage;

import io.github.crownalonedev.bloom.storage.internal.Jdbc;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import javax.sql.DataSource;

/**
 * Synchronous database access for code that already runs off the main thread (for example inside
 * {@code scheduler().async(...)} or a {@code CompletableFuture} chain). Obtain with {@link Database#blocking()}.
 *
 * <p>Every method throws {@link IllegalStateException} when called on the main server thread (as reported by the
 * {@code mainThreadCheck} given to the database builder), because blocking JDBC calls there freeze the server.
 *
 * @since 0.1.0
 */
public final class BlockingDatabase implements SqlOperations {

    private final DataSource dataSource;
    private final BooleanSupplier mainThread;

    BlockingDatabase(DataSource dataSource, BooleanSupplier mainThread) {
        this.dataSource = dataSource;
        this.mainThread = mainThread;
    }

    private Connection connection(String operation) {
        if (mainThread.getAsBoolean()) {
            throw new IllegalStateException("BlockingDatabase." + operation + " was called on the main server thread. "
                    + "Blocking database calls there freeze the server; use the asynchronous Database methods, or run "
                    + "this code in scheduler().async(...).");
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new StorageException("Could not obtain a database connection: " + e.getMessage(), e);
        }
    }

    @Override
    public int update(String sql, Object... params) {
        try (Connection c = connection("update")) {
            return Jdbc.update(c, sql, params);
        } catch (SQLException e) {
            throw Jdbc.failure(sql, e);
        }
    }

    @Override
    public Optional<Long> insert(String sql, Object... params) {
        try (Connection c = connection("insert")) {
            return Jdbc.insert(c, sql, params);
        } catch (SQLException e) {
            throw Jdbc.failure(sql, e);
        }
    }

    @Override
    public int[] batch(String sql, List<Object[]> rows) {
        try (Connection c = connection("batch")) {
            return Jdbc.batch(c, sql, rows);
        } catch (SQLException e) {
            throw Jdbc.failure(sql, e);
        }
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = connection("query")) {
            return Jdbc.query(c, sql, mapper, params);
        } catch (SQLException e) {
            throw Jdbc.failure(sql, e);
        }
    }

    @Override
    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = connection("queryOne")) {
            return Jdbc.queryOne(c, sql, mapper, params);
        } catch (SQLException e) {
            throw Jdbc.failure(sql, e);
        }
    }
}
