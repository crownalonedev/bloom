package io.github.crownalonedev.bloom.api.scheduler;

import io.github.crownalonedev.bloom.api.lifecycle.Terminable;

/**
 * Handle to a task submitted to a {@link BloomScheduler}.
 *
 * <p>{@link #close()} is equivalent to {@link #cancel()}.
 *
 * @since 0.1.0
 */
public interface ScheduledTask extends Terminable {

    /**
     * Cancels the task. A one-shot task that already ran is unaffected; a repeating task will not run again.
     * Idempotent.
     */
    void cancel();

    /**
     * Returns whether the task was cancelled (explicitly or by scheduler shutdown).
     *
     * @return {@code true} if cancelled
     */
    boolean isCancelled();

    /**
     * Returns whether a one-shot task has finished running. Always {@code false} for repeating tasks that were not
     * cancelled.
     *
     * @return {@code true} if finished
     */
    boolean isDone();

    /**
     * Returns whether the task runs off the main server thread.
     *
     * @return {@code true} for async tasks
     */
    boolean isAsync();

    /**
     * Returns whether the task repeats.
     *
     * @return {@code true} for repeating tasks
     */
    boolean isRepeating();

    @Override
    default void close() {
        cancel();
    }

    @Override
    default boolean isClosed() {
        return isCancelled() || isDone();
    }
}
