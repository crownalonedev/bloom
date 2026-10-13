package io.github.crownalonedev.bloom.storage;

import io.github.crownalonedev.bloom.api.BloomException;

/**
 * Thrown (or used to complete futures exceptionally) when a database operation fails. The message includes the SQL
 * statement (without parameter values) when relevant.
 *
 * @since 0.1.0
 */
public class StorageException extends BloomException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception.
     *
     * @param message description
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a cause.
     *
     * @param message description
     * @param cause cause, usually a {@link java.sql.SQLException}
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
