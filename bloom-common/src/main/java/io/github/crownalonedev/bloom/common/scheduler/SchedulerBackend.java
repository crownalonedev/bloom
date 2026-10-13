package io.github.crownalonedev.bloom.common.scheduler;

/**
 * Minimal platform scheduling primitives. {@link TrackingScheduler} adds ownership, error handling and shutdown on
 * top. Implementations: the Bukkit scheduler (bloom-bukkit) and a manual tick-driven backend (bloom-testing).
 */
public interface SchedulerBackend {

    /**
     * Returns whether the calling thread is the main server thread.
     *
     * @return {@code true} on the main thread
     */
    boolean isPrimaryThread();

    /**
     * Schedules a main-thread task.
     *
     * @param task task
     * @param delayTicks delay; {@code 0} means next tick
     * @param periodTicks period, or {@code -1} for a one-shot task
     * @return cancellation handle
     */
    Handle scheduleSync(Runnable task, long delayTicks, long periodTicks);

    /**
     * Schedules a background task.
     *
     * @param task task
     * @param delayTicks delay; {@code 0} means as soon as possible
     * @param periodTicks period, or {@code -1} for a one-shot task
     * @return cancellation handle
     */
    Handle scheduleAsync(Runnable task, long delayTicks, long periodTicks);

    /** Cancellation handle returned by the backend. */
    interface Handle {
        /** Cancels the underlying platform task. Must be idempotent and safe to call from any thread. */
        void cancel();
    }
}
