package io.github.crownalonedev.bloom.api.scheduler;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Plugin-owned task scheduling with automatic cleanup.
 *
 * <p>Every task is owned by the Bloom instance that created it and is cancelled when Bloom shuts down.
 * Exceptions thrown by tasks are caught and logged with the task's origin; they never kill the scheduler.
 *
 * <h2>Threading rules</h2>
 *
 * <ul>
 *   <li>{@code sync*} tasks run on the main server thread. Use them for anything that touches worlds, entities,
 *       players, inventories or most other Bukkit API.
 *   <li>{@code async*} tasks run on a background thread. Use them for I/O, computation and network calls.
 *       Never modify worlds, entities or inventories from an async task: hop back with {@link #sync(Runnable)}
 *       or {@link #mainThreadExecutor()}.
 * </ul>
 *
 * <p>Delays and periods are in server ticks (20 per second at full speed) unless a {@link Duration} overload is
 * used. A delay of {@code 0} means "the next tick".
 *
 * @since 0.1.0
 */
public interface BloomScheduler {

    /**
     * Returns whether the calling thread is the main server thread.
     *
     * @return {@code true} on the main thread
     */
    boolean isPrimaryThread();

    /**
     * Throws if not called from the main server thread. Use at the start of methods that touch game state.
     *
     * @param operation what the caller is doing, used in the error message
     * @throws IllegalStateException if called from another thread
     */
    void ensurePrimaryThread(String operation);

    /**
     * Runs a task on the main thread on the next tick.
     *
     * @param task task to run
     * @return task handle
     */
    ScheduledTask sync(Runnable task);

    /**
     * Runs a task on the main thread after a delay.
     *
     * @param task task to run
     * @param delayTicks delay in ticks, {@code >= 0}
     * @return task handle
     */
    ScheduledTask syncLater(Runnable task, long delayTicks);

    /**
     * Runs a task on the main thread after a delay.
     *
     * @param task task to run
     * @param delay delay, rounded up to whole ticks
     * @return task handle
     */
    ScheduledTask syncLater(Runnable task, Duration delay);

    /**
     * Runs a task repeatedly on the main thread. The task receives its own handle so it can cancel itself.
     *
     * @param task task to run; receives its handle
     * @param delayTicks initial delay in ticks, {@code >= 0}
     * @param periodTicks period in ticks, {@code >= 1}
     * @return task handle
     */
    ScheduledTask syncRepeating(Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    /**
     * Runs a task repeatedly on the main thread.
     *
     * @param task task to run; receives its handle
     * @param delay initial delay
     * @param period period, at least one tick
     * @return task handle
     */
    ScheduledTask syncRepeating(Consumer<ScheduledTask> task, Duration delay, Duration period);

    /**
     * Runs a task on a background thread as soon as possible.
     *
     * @param task task to run; must not touch game state
     * @return task handle
     */
    ScheduledTask async(Runnable task);

    /**
     * Runs a task on a background thread after a delay.
     *
     * @param task task to run; must not touch game state
     * @param delayTicks delay in ticks
     * @return task handle
     */
    ScheduledTask asyncLater(Runnable task, long delayTicks);

    /**
     * Runs a task repeatedly on a background thread.
     *
     * @param task task to run; receives its handle; must not touch game state
     * @param delayTicks initial delay in ticks
     * @param periodTicks period in ticks, {@code >= 1}
     * @return task handle
     */
    ScheduledTask asyncRepeating(Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    /**
     * Computes a value on the main thread. If already on the main thread the supplier still runs on the next tick,
     * so the behaviour is the same from every caller.
     *
     * @param supplier computation; may touch game state
     * @param <T> result type
     * @return future completed on the main thread with the result or the thrown exception
     */
    <T> CompletableFuture<T> supplySync(Supplier<T> supplier);

    /**
     * Computes a value on a background thread.
     *
     * @param supplier computation; must not touch game state
     * @param <T> result type
     * @return future completed with the result or the thrown exception
     */
    <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier);

    /**
     * Returns an executor that runs tasks on the main thread. Useful for
     * {@code future.thenAcceptAsync(result -> ..., scheduler.mainThreadExecutor())}.
     *
     * @return main-thread executor owned by this scheduler
     */
    Executor mainThreadExecutor();

    /**
     * Returns an executor that runs tasks on background threads owned by this scheduler.
     *
     * @return async executor
     */
    Executor asyncExecutor();

    /**
     * Returns the number of tasks that are scheduled and not yet finished or cancelled.
     *
     * @return active task count
     */
    int activeTaskCount();
}
