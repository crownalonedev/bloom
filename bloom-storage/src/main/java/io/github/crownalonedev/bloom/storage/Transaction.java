package io.github.crownalonedev.bloom.storage;

import java.sql.Connection;

/**
 * Operations inside a database transaction. All statements share one connection; the transaction commits when the
 * callback returns and rolls back if it throws. Only valid inside {@link Database#transaction}.
 *
 * @since 0.1.0
 */
public interface Transaction extends SqlOperations {

    /**
     * Returns the database dialect.
     *
     * @return dialect
     */
    Dialect dialect();

    /**
     * Returns the underlying connection for advanced JDBC use. Do not close it or change auto-commit.
     *
     * @return connection
     */
    Connection connection();
}
