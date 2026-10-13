package io.github.crownalonedev.bloom.storage;

import java.util.List;
import java.util.Optional;

/**
 * Synchronous SQL operations. Implemented by {@link Transaction} (one connection, inside
 * {@link Database#transaction}) and {@link BlockingDatabase} (for code already running off the main thread).
 *
 * <h2>Parameters</h2>
 * {@code ?} placeholders are bound in order. Supported parameter types: {@code null}, {@code String}, numbers,
 * {@code Boolean}, {@code byte[]}, {@code UUID} (stored as its 36-character string), enums (stored by
 * {@code name()}), {@code java.time.Instant} (stored as epoch milliseconds, a {@code BIGINT}), {@code Duration}
 * (milliseconds) and any type the JDBC driver accepts in {@code setObject}.
 *
 * @since 0.1.0
 */
public interface SqlOperations {

    /**
     * Executes an INSERT, UPDATE, DELETE or DDL statement.
     *
     * @param sql statement with {@code ?} placeholders
     * @param params parameter values
     * @return affected row count
     * @throws StorageException on failure
     */
    int update(String sql, Object... params);

    /**
     * Executes an INSERT and returns the generated key.
     *
     * @param sql insert statement
     * @param params parameter values
     * @return generated key, or empty if the statement generated none
     * @throws StorageException on failure
     */
    Optional<Long> insert(String sql, Object... params);

    /**
     * Executes a statement once per parameter row, as a JDBC batch.
     *
     * @param sql statement with {@code ?} placeholders
     * @param rows parameter rows
     * @return affected row counts per row (drivers may report {@code -2}, "success, count unknown")
     * @throws StorageException on failure
     */
    int[] batch(String sql, List<Object[]> rows);

    /**
     * Runs a query and maps every row.
     *
     * @param sql query
     * @param mapper row mapper
     * @param params parameter values
     * @param <T> row type
     * @return mapped rows, possibly empty
     * @throws StorageException on failure
     */
    <T> List<T> query(String sql, RowMapper<T> mapper, Object... params);

    /**
     * Runs a query expected to return at most one row.
     *
     * @param sql query
     * @param mapper row mapper
     * @param params parameter values
     * @param <T> row type
     * @return the first row, or empty
     * @throws StorageException on failure
     */
    <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params);
}
