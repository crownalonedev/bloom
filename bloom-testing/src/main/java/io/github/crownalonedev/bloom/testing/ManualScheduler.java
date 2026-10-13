package io.github.crownalonedev.bloom.testing;

import io.github.crownalonedev.bloom.api.scheduler.BloomScheduler;
import io.github.crownalonedev.bloom.api.scheduler.ScheduledTask;
import io.github.crownalonedev.bloom.common.scheduler.SchedulerBackend;
import io.github.crownalonedev.bloom.common.scheduler.TrackingScheduler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * A deterministic {@link BloomScheduler} for unit tests. Nothing runs until you advance time with {@link #tick()} /
 * {@link #tick(long)}; "async" tasks also run on the test thread during ticks, so tests need no sleeps or latches.
 * It has the same ownership, error-isolation and cancellation behaviour as the real Bloom scheduler.
 *
 * <pre>{@code
 * ManualScheduler scheduler = ManualScheduler.create();
 * MyCountdown countdown = new MyCountdown(scheduler); // code under test takes a BloomScheduler
 * countdown.start(3);
 * scheduler.tick(60);
 * assertTrue(countdown.isFinished());
 * }</pre>
 *
 * <p>Not thread-safe: use from the test thread only.
 *
 * @since 0.1.0
 */
public final class ManualScheduler implements BloomScheduler, AutoCloseable {

    private final Backend backend;
    private final TrackingScheduler delegate;

    private ManualScheduler(Backend backend, Logger logger) {
        this.backend = backend;
        this.delegate = new TrackingScheduler(backend, logger, "ManualScheduler");
    }

    /**
     * Creates a scheduler at tick 0 whose calling thread counts as the main thread.
     *
     * @return scheduler
     */
    public static ManualScheduler create() {
        return new ManualScheduler(new Backend(), Logger.getLogger("Bloom-ManualScheduler"));
    }

    /**
     * Creates a scheduler that logs task failures to a specific logger.
     *
     * @param logger logger for task exceptions
     * @return scheduler
     */
    public static ManualScheduler create(Logger logger) {
        return new ManualScheduler(new Backend(), logger);
    }

    /** Advances one tick, running every task that is due. */
    public void tick() {
        backend.advance();
    }

    /**
     * Advances several ticks.
     *
     * @param ticks number of ticks
     */
    public void tick(long ticks) {
        for (long i = 0; i < ticks; i++) {
            backend.advance();
        }
    }

    /**
     * Advances by a duration (rounded up to whole ticks).
     *
     * @param duration time to advance
     */
    public void advance(Duration duration) {
        tick(io.github.crownalonedev.bloom.api.scheduler.Ticks.from(duration));
    }

    /**
     * Returns the current tick.
     *
     * @return ticks advanced so far
     */
    public long currentTick() {
        return backend.tick;
    }

    /**
     * Sets what {@link #isPrimaryThread()} reports, to test code paths that require (or forbid) the main thread.
     *
     * @param primary value to report
     */
    public void primaryThread(boolean primary) {
        backend.primary = primary;
    }

    @Override
    public boolean isPrimaryThread() {
        return delegate.isPrimaryThread();
    }

    @Override
    public void ensurePrimaryThread(String operation) {
        delegate.ensurePrimaryThread(operation);
    }

    @Override
    public ScheduledTask sync(Runnable task) {
        return delegate.sync(task);
    }

    @Override
    public ScheduledTask syncLater(Runnable task, long delayTicks) {
        return delegate.syncLater(task, delayTicks);
    }

    @Override
    public ScheduledTask syncLater(Runnable task, Duration delay) {
        return delegate.syncLater(task, delay);
    }

    @Override
    public ScheduledTask syncRepeating(Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        return delegate.syncRepeating(task, delayTicks, periodTicks);
    }

    @Override
    public ScheduledTask syncRepeating(Consumer<ScheduledTask> task, Duration delay, Duration period) {
        return delegate.syncRepeating(task, delay, period);
    }

    @Override
    public ScheduledTask async(Runnable task) {
        return delegate.async(task);
    }

    @Override
    public ScheduledTask asyncLater(Runnable task, long delayTicks) {
        return delegate.asyncLater(task, delayTicks);
    }

    @Override
    public ScheduledTask asyncRepeating(Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        return delegate.asyncRepeating(task, delayTicks, periodTicks);
    }

    @Override
    public <T> CompletableFuture<T> supplySync(Supplier<T> supplier) {
        return delegate.supplySync(supplier);
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return delegate.supplyAsync(supplier);
    }

    @Override
    public Executor mainThreadExecutor() {
        return delegate.mainThreadExecutor();
    }

    @Override
    public Executor asyncExecutor() {
        return delegate.asyncExecutor();
    }

    @Override
    public int activeTaskCount() {
        return delegate.activeTaskCount();
    }

    /** Cancels all tasks, like Bloom does on shutdown. */
    @Override
    public void close() {
        delegate.close();
    }

    /** Tick-driven backend; a delay of 0 means "next tick", matching Bukkit. */
    private static final class Backend implements SchedulerBackend {
        private final List<Entry> entries = new ArrayList<Entry>();
        long tick;
        boolean primary = true;

        @Override
        public boolean isPrimaryThread() {
            return primary;
        }

        @Override
        public Handle scheduleSync(Runnable task, long delayTicks, long periodTicks) {
            Entry entry = new Entry(task, tick + Math.max(1, delayTicks), periodTicks);
            entries.add(entry);
            return entry;
        }

        @Override
        public Handle scheduleAsync(Runnable task, long delayTicks, long periodTicks) {
            return scheduleSync(task, delayTicks, periodTicks);
        }

        void advance() {
            tick++;
            for (Entry entry : new ArrayList<Entry>(entries)) {
                if (!entry.cancelled && entry.nextRun <= tick) {
                    entry.task.run();
                    if (entry.period > 0) {
                        entry.nextRun = tick + entry.period;
                    } else {
                        entry.cancelled = true;
                    }
                }
            }
            for (Iterator<Entry> it = entries.iterator(); it.hasNext(); ) {
                if (it.next().cancelled) {
                    it.remove();
                }
            }
        }

        private static final class Entry implements Handle {
            final Runnable task;
            final long period;
            long nextRun;
            boolean cancelled;

            Entry(Runnable task, long nextRun, long period) {
                this.task = task;
                this.nextRun = nextRun;
                this.period = period;
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        }
    }
}
