package io.github.crownalonedev.bloom.common.scheduler;

import io.github.crownalonedev.bloom.api.lifecycle.Terminable;
import io.github.crownalonedev.bloom.api.scheduler.BloomScheduler;
import io.github.crownalonedev.bloom.api.scheduler.ScheduledTask;
import io.github.crownalonedev.bloom.api.scheduler.Ticks;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link BloomScheduler} implementation that tracks every task it creates, isolates task failures and cancels
 * everything on {@link #close()}.
 */
public final class TrackingScheduler implements BloomScheduler, Terminable {

    private final SchedulerBackend backend;
    private final Logger logger;
    private final String ownerName;
    private final Set<Task> active = ConcurrentHashMap.newKeySet();
    private final Executor mainExecutor = this::sync;
    private final Executor asyncExecutor = this::async;
    private volatile boolean closed;

    /**
     * Creates a scheduler.
     *
     * @param backend platform primitives
     * @param logger receives task failures
     * @param ownerName owner shown in error messages, usually the plugin name
     */
    public TrackingScheduler(SchedulerBackend backend, Logger logger, String ownerName) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.ownerName = Objects.requireNonNull(ownerName, "ownerName");
    }

    @Override
    public boolean isPrimaryThread() {
        return backend.isPrimaryThread();
    }

    @Override
    public void ensurePrimaryThread(String operation) {
        if (!backend.isPrimaryThread()) {
            throw new IllegalStateException(operation + " must be called from the main server thread, but was "
                    + "called from thread '" + Thread.currentThread().getName() + "'. Switch threads with "
                    + "scheduler().sync(...) or future.thenAcceptAsync(..., scheduler().mainThreadExecutor()).");
        }
    }

    @Override
    public ScheduledTask sync(Runnable task) {
        return submit(false, wrap(task), 0, -1);
    }

    @Override
    public ScheduledTask syncLater(Runnable task, long delayTicks) {
        return submit(false, wrap(task), checkDelay(delayTicks), -1);
    }

    @Override
    public ScheduledTask syncLater(Runnable task, Duration delay) {
        return syncLater(task, Ticks.from(delay));
    }

    @Override
    public ScheduledTask syncRepeating(Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        return submit(false, Objects.requireNonNull(task, "task"), checkDelay(delayTicks), checkPeriod(periodTicks));
    }

    @Override
    public ScheduledTask syncRepeating(Consumer<ScheduledTask> task, Duration delay, Duration period) {
        return syncRepeating(task, Ticks.from(delay), Math.max(1, Ticks.from(period)));
    }

    @Override
    public ScheduledTask async(Runnable task) {
        return submit(true, wrap(task), 0, -1);
    }

    @Override
    public ScheduledTask asyncLater(Runnable task, long delayTicks) {
        return submit(true, wrap(task), checkDelay(delayTicks), -1);
    }

    @Override
    public ScheduledTask asyncRepeating(Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        return submit(true, Objects.requireNonNull(task, "task"), checkDelay(delayTicks), checkPeriod(periodTicks));
    }

    @Override
    public <T> CompletableFuture<T> supplySync(Supplier<T> supplier) {
        return supply(false, supplier);
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return supply(true, supplier);
    }

    private <T> CompletableFuture<T> supply(boolean async, final Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        final CompletableFuture<T> future = new CompletableFuture<T>();
        Task task = submit(
                async,
                new Consumer<ScheduledTask>() {
                    @Override
                    public void accept(ScheduledTask handle) {
                        try {
                            future.complete(supplier.get());
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    }
                },
                0,
                -1);
        task.onCancel = new Runnable() {
            @Override
            public void run() {
                future.completeExceptionally(new CancellationException(
                        "Task was cancelled before it ran " + "(scheduler for " + ownerName + " shut down)."));
            }
        };
        if (task.isCancelled()) {
            task.onCancel.run();
        }
        return future;
    }

    @Override
    public Executor mainThreadExecutor() {
        return mainExecutor;
    }

    @Override
    public Executor asyncExecutor() {
        return asyncExecutor;
    }

    @Override
    public int activeTaskCount() {
        return active.size();
    }

    /** Cancels all tasks and rejects new submissions. Idempotent. */
    @Override
    public void close() {
        closed = true;
        List<Task> snapshot = new ArrayList<Task>(active);
        for (Task task : snapshot) {
            task.cancel();
        }
        active.clear();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    private Task submit(boolean async, Consumer<ScheduledTask> body, long delay, long period) {
        if (closed) {
            throw new IllegalStateException(
                    "Cannot schedule a task: the Bloom scheduler for " + ownerName + " has shut down.");
        }
        Task task = new Task(async, period > 0, body);
        active.add(task);
        SchedulerBackend.Handle handle =
                async ? backend.scheduleAsync(task, delay, period) : backend.scheduleSync(task, delay, period);
        task.attach(handle);
        if (closed) {
            task.cancel();
        }
        return task;
    }

    private static Consumer<ScheduledTask> wrap(final Runnable runnable) {
        Objects.requireNonNull(runnable, "task");
        return new Consumer<ScheduledTask>() {
            @Override
            public void accept(ScheduledTask handle) {
                runnable.run();
            }
        };
    }

    private static long checkDelay(long delayTicks) {
        if (delayTicks < 0) {
            throw new IllegalArgumentException("Delay must be >= 0 ticks, got " + delayTicks);
        }
        return delayTicks;
    }

    private static long checkPeriod(long periodTicks) {
        if (periodTicks < 1) {
            throw new IllegalArgumentException("Period must be >= 1 tick, got " + periodTicks);
        }
        return periodTicks;
    }

    private final class Task implements ScheduledTask, Runnable {
        private final boolean async;
        private final boolean repeating;
        private final Consumer<ScheduledTask> body;
        private SchedulerBackend.Handle handle;
        private volatile boolean cancelled;
        private volatile boolean done;
        volatile Runnable onCancel;

        Task(boolean async, boolean repeating, Consumer<ScheduledTask> body) {
            this.async = async;
            this.repeating = repeating;
            this.body = body;
        }

        void attach(SchedulerBackend.Handle backendHandle) {
            boolean cancelNow;
            synchronized (this) {
                handle = backendHandle;
                cancelNow = cancelled;
            }
            if (cancelNow) {
                backendHandle.cancel();
            }
        }

        @Override
        public void run() {
            if (cancelled || done) {
                return;
            }
            try {
                body.accept(this);
            } catch (Throwable t) {
                logger.log(
                        Level.SEVERE,
                        "Uncaught exception in " + (async ? "async" : "sync") + (repeating ? " repeating" : "")
                                + " task owned by " + ownerName,
                        t);
            } finally {
                if (!repeating) {
                    done = true;
                    active.remove(this);
                }
            }
        }

        @Override
        public void cancel() {
            SchedulerBackend.Handle toCancel;
            synchronized (this) {
                if (cancelled || done) {
                    return;
                }
                cancelled = true;
                toCancel = handle;
            }
            active.remove(this);
            if (toCancel != null) {
                toCancel.cancel();
            }
            Runnable callback = onCancel;
            if (callback != null) {
                callback.run();
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean isAsync() {
            return async;
        }

        @Override
        public boolean isRepeating() {
            return repeating;
        }
    }
}
