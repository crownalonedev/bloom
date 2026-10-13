package io.github.crownalonedev.bloom.storage;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps the current row of a {@link ResultSet} to an object. Do not call {@code next()} inside the mapper.
 *
 * <pre>{@code
 * RowMapper<PlayerStats> mapper = rs -> new PlayerStats(
 *         UUID.fromString(rs.getString("uuid")), rs.getInt("kills"), rs.getInt("deaths"));
 * }</pre>
 *
 * @param <T> result type
 * @since 0.1.0
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the current row.
     *
     * @param row result set positioned on a row
     * @return mapped object
     * @throws SQLException on column access errors
     */
    T map(ResultSet row) throws SQLException;
}
