package io.github.crownalonedev.bloom.bukkit.event;

import io.github.crownalonedev.bloom.api.lifecycle.Terminable;

/**
 * An active event registration. {@link #close()} is equivalent to {@link #unsubscribe()}.
 *
 * @since 0.1.0
 */
public interface Subscription extends Terminable {

    /** Stops receiving events. Idempotent and safe from any thread. */
    void unsubscribe();

    /**
     * Returns whether the subscription still receives events.
     *
     * @return {@code true} until unsubscribed
     */
    boolean isActive();

    /**
     * Returns how many events the handler has processed.
     *
     * @return handled event count
     */
    long handledCount();

    @Override
    default void close() {
        unsubscribe();
    }

    @Override
    default boolean isClosed() {
        return !isActive();
    }
}
