package io.github.crownalonedev.bloom.storage.internal;

import io.github.crownalonedev.bloom.storage.RowMapper;
import io.github.crownalonedev.bloom.storage.StorageException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Statement execution on a given connection. Internal. */
public final class Jdbc {

    private Jdbc() {}

    /**
     * Binds parameters to a prepared statement.
     *
     * @param statement statement
     * @param params values
     * @throws SQLException on binding errors
     */
    public static void bind(PreparedStatement statement, Object[] params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            int index = i + 1;
            Object value = params[i];
            if (value == null) {
                statement.setNull(index, Types.NULL);
            } else if (value instanceof UUID) {
                statement.setString(index, value.toString());
            } else if (value instanceof Enum) {
                statement.setString(index, ((Enum<?>) value).name());
            } else if (value instanceof Instant) {
                statement.setLong(index, ((Instant) value).toEpochMilli());
            } else if (value instanceof Duration) {
                statement.setLong(index, ((Duration) value).toMillis());
            } else if (value instanceof Boolean) {
                statement.setBoolean(index, (Boolean) value);
            } else if (value instanceof byte[]) {
                statement.setBytes(index, (byte[]) value);
            } else {
                statement.setObject(index, value);
            }
        }
    }

    public static int update(Connection connection, String sql, Object[] params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    public static Optional<Long> insert(Connection connection, String sql, Object[] params) {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, params);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? Optional.of(keys.getLong(1)) : Optional.<Long>empty();
            }
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    public static int[] batch(Connection connection, String sql, List<Object[]> rows) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Object[] row : rows) {
                bind(statement, row);
                statement.addBatch();
            }
            return statement.executeBatch();
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    public static <T> List<T> query(Connection connection, String sql, RowMapper<T> mapper, Object[] params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet rows = statement.executeQuery()) {
                List<T> result = new ArrayList<T>();
                while (rows.next()) {
                    result.add(mapper.map(rows));
                }
                return result;
            }
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    public static <T> Optional<T> queryOne(Connection connection, String sql, RowMapper<T> mapper, Object[] params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            statement.setMaxRows(1);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.ofNullable(mapper.map(rows)) : Optional.<T>empty();
            }
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    /**
     * Wraps an SQL exception with the statement (never parameter values, which may contain personal data).
     *
     * @param sql statement
     * @param e cause
     * @return exception to throw
     */
    public static StorageException failure(String sql, SQLException e) {
        return new StorageException(
                "SQL failed (" + e.getSQLState() + "): " + e.getMessage() + " — statement: " + abbreviate(sql), e);
    }

    private static String abbreviate(String sql) {
        String single = sql.replaceAll("\\s+", " ").trim();
        return single.length() > 300 ? single.substring(0, 300) + "..." : single;
    }
}
