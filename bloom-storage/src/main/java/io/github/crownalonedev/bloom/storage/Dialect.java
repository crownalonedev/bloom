package io.github.crownalonedev.bloom.storage;

import java.util.Arrays;
import java.util.List;

/**
 * SQL dialects Bloom knows about. Used for driver defaults and small SQL helpers; Bloom does not rewrite your SQL.
 *
 * @since 0.1.0
 */
public enum Dialect {
    /** SQLite (file database). */
    SQLITE(Arrays.asList("org.sqlite.JDBC")),
    /** MySQL. Bloom tries Connector/J 8+ first, then the 5.x driver bundled with legacy servers. */
    MYSQL(Arrays.asList("com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver")),
    /** MariaDB (Connector/J must be shaded into your plugin). */
    MARIADB(Arrays.asList("org.mariadb.jdbc.Driver"));

    private final List<String> driverClasses;

    Dialect(List<String> driverClasses) {
        this.driverClasses = driverClasses;
    }

    /**
     * Returns candidate JDBC driver classes, in preference order.
     *
     * @return driver class names
     */
    public List<String> driverClasses() {
        return driverClasses;
    }

    /**
     * Builds an insert-or-update statement with {@code ?} placeholders for every column, in order.
     *
     * <ul>
     *   <li>SQLite: {@code INSERT OR REPLACE} (works on the old SQLite bundled with 1.8 servers; note that it deletes
     *       and re-inserts the row, so {@code ON DELETE CASCADE} children are removed)
     *   <li>MySQL/MariaDB: {@code INSERT ... ON DUPLICATE KEY UPDATE}
     * </ul>
     *
     * @param table table name
     * @param columns all columns, primary key columns included
     * @param keyColumns primary/unique key columns (not updated on conflict)
     * @return SQL
     */
    public String upsert(String table, List<String> columns, List<String> keyColumns) {
        StringBuilder names = new StringBuilder();
        StringBuilder marks = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                names.append(", ");
                marks.append(", ");
            }
            names.append(columns.get(i));
            marks.append('?');
        }
        if (this == SQLITE) {
            return "INSERT OR REPLACE INTO " + table + " (" + names + ") VALUES (" + marks + ")";
        }
        StringBuilder updates = new StringBuilder();
        for (String column : columns) {
            if (keyColumns.contains(column)) {
                continue;
            }
            if (updates.length() > 0) {
                updates.append(", ");
            }
            updates.append(column).append(" = VALUES(").append(column).append(')');
        }
        if (updates.length() == 0) {
            return "INSERT IGNORE INTO " + table + " (" + names + ") VALUES (" + marks + ")";
        }
        return "INSERT INTO " + table + " (" + names + ") VALUES (" + marks + ") ON DUPLICATE KEY UPDATE " + updates;
    }
}
