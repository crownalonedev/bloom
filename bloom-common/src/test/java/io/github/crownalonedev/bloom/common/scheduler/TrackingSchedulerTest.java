package io.github.crownalonedev.bloom.common.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crownalonedev.bloom.api.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrackingSchedulerTest {

    /** Single-threaded tick-driven backend (sync and async both run on tick for determinism). */
    static class TickBackend implements SchedulerBackend {
        final List<Entry> entries = new ArrayList<Entry>();
        long tick;

        static final class Entry implements Handle {
            final Runnable task;
            long nextRun;
            final long period;
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

        @Override
        public boolean isPrimaryThread() {
            return true;
        }

        @Override
        public Handle scheduleSync(Runnable task, long delayTicks, long periodTicks) {
            Entry e = new Entry(task, tick + Math.max(1, delayTicks), periodTicks);
            entries.add(e);
            return e;
        }

        @Override
        public Handle scheduleAsync(Runnable task, long delayTicks, long periodTicks) {
            return scheduleSync(task, delayTicks, periodTicks);
        }

        void tick(int count) {
            for (int i = 0; i < count; i++) {
                tick++;
                for (Entry e : new ArrayList<Entry>(entries)) {
                    if (!e.cancelled && e.nextRun <= tick) {
                        e.task.run();
                        if (e.period > 0) {
                            e.nextRun = tick + e.period;
                        } else {
                            e.cancelled = true;
                        }
                    }
                }
                for (Iterator<Entry> it = entries.iterator(); it.hasNext(); ) {
                    if (it.next().cancelled) {
                        it.remove();
                    }
                }
            }
        }
    }

    private TickBackend backend;
    private TrackingScheduler scheduler;
    private final List<LogRecord> logs = new ArrayList<LogRecord>();

    @BeforeEach
    void setUp() {
        backend = new TickBackend();
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logs.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
        scheduler = new TrackingScheduler(backend, logger, "TestPlugin");
    }

    @Test
    void oneShotTaskRunsOnceAndIsUntracked() {
        AtomicInteger runs = new AtomicInteger();
        ScheduledTask task = scheduler.syncLater(runs::incrementAndGet, 3);
        assertEquals(1, scheduler.activeTaskCount());
        backend.tick(2);
        assertEquals(0, runs.get());
        backend.tick(1);
        assertEquals(1, runs.get());
        assertTrue(task.isDone());
        assertEquals(0, scheduler.activeTaskCount());
    }

    @Test
    void repeatingTaskCanCancelItself() {
        AtomicInteger runs = new AtomicInteger();
        ScheduledTask task = scheduler.syncRepeating(
                handle -> {
                    if (runs.incrementAndGet() == 3) {
                        handle.cancel();
                    }
                },
                0,
                2);
        backend.tick(20);
        assertEquals(3, runs.get());
        assertTrue(task.isCancelled());
        assertEquals(0, scheduler.activeTaskCount());
    }

    @Test
    void exceptionsAreLoggedAndIsolated() {
        AtomicInteger after = new AtomicInteger();
        scheduler.sync(() -> {
            throw new IllegalStateException("boom");
        });
        scheduler.sync(after::incrementAndGet);
        backend.tick(1);
        assertEquals(1, after.get());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).getMessage().contains("TestPlugin"));
    }

    @Test
    void closeCancelsEverythingAndRejectsNewTasks() {
        AtomicInteger runs = new AtomicInteger();
        scheduler.syncRepeating(h -> runs.incrementAndGet(), 0, 1);
        CompletableFuture<String> pending = scheduler.supplySync(() -> "never");
        scheduler.close();
        backend.tick(5);
        assertEquals(0, runs.get());
        assertEquals(0, scheduler.activeTaskCount());
        assertThrows(CancellationException.class, pending::get);
        assertTrue(pending.isCompletedExceptionally());
        assertThrows(IllegalStateException.class, () -> scheduler.sync(() -> {}));
    }

    @Test
    void supplyCompletesWithValueOrException() throws Exception {
        CompletableFuture<Integer> ok = scheduler.supplyAsync(() -> 7);
        CompletableFuture<Integer> failed = scheduler.supplySync(() -> {
            throw new IllegalArgumentException("bad");
        });
        backend.tick(1);
        assertEquals(Integer.valueOf(7), ok.get());
        assertTrue(failed.isCompletedExceptionally());
    }

    @Test
    void argumentValidation() {
        assertThrows(IllegalArgumentException.class, () -> scheduler.syncLater(() -> {}, -1));
        assertThrows(IllegalArgumentException.class, () -> scheduler.syncRepeating(h -> {}, 0, 0));
    }

    @Test
    void ensurePrimaryThreadMessageIsHelpful() {
        SchedulerBackend offThread = new TickBackend() {
            @Override
            public boolean isPrimaryThread() {
                return false;
            }
        };
        TrackingScheduler s = new TrackingScheduler(offThread, Logger.getAnonymousLogger(), "P");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> s.ensurePrimaryThread("Open menu"));
        assertTrue(e.getMessage().startsWith("Open menu must be called from the main server thread"));
        assertFalse(e.getMessage().isEmpty());
    }
}
