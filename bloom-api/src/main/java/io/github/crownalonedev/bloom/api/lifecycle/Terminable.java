package io.github.crownalonedev.bloom.api.lifecycle;

/**
 * A resource that can be closed, such as a scheduled task, an event subscription or a service.
 *
 * <p>Unlike {@link AutoCloseable#close()}, {@link #close()} does not throw checked exceptions and must be
 * idempotent: closing twice has no further effect.
 *
 * @since 0.1.0
 */
public interface Terminable extends AutoCloseable {

    /** Releases the resource. Calling this more than once has no further effect. */
    @Override
    void close();

    /**
     * Returns whether this resource has been closed.
     *
     * @return {@code true} once {@link #close()} has completed
     */
    boolean isClosed();
}
