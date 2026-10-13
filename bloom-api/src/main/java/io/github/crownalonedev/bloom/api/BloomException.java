package io.github.crownalonedev.bloom.api;

/**
 * Base class for all exceptions thrown by Bloom.
 *
 * <p>Bloom exceptions are unchecked and carry developer-facing messages that explain what went wrong and,
 * where possible, how to fix it.
 *
 * @since 0.1.0
 */
public class BloomException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message.
     *
     * @param message developer-facing description of the problem
     */
    public BloomException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and cause.
     *
     * @param message developer-facing description of the problem
     * @param cause underlying cause
     */
    public BloomException(String message, Throwable cause) {
        super(message, cause);
    }
}
