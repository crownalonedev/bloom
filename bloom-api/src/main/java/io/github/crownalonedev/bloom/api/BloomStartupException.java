package io.github.crownalonedev.bloom.api;

/**
 * Thrown when Bloom cannot start, for example because the server version is unsupported or the required
 * platform adapter is missing from the plugin jar.
 *
 * <p>The message always names the detected platform and the corrective action.
 *
 * @since 0.1.0
 */
public class BloomStartupException extends BloomException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a startup exception.
     *
     * @param message developer-facing description of the problem and fix
     */
    public BloomStartupException(String message) {
        super(message);
    }

    /**
     * Creates a startup exception with a cause.
     *
     * @param message developer-facing description of the problem and fix
     * @param cause underlying cause
     */
    public BloomStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}
