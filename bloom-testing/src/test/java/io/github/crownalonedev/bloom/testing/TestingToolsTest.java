package io.github.crownalonedev.bloom.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crownalonedev.bloom.api.util.Cooldowns;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TestingToolsTest {

    @Test
    void manualSchedulerRunsTasksOnlyWhenTicked() throws Exception {
        ManualScheduler scheduler = ManualScheduler.create();
        AtomicInteger counter = new AtomicInteger();
        scheduler.syncLater(counter::incrementAndGet, Duration.ofSeconds(1));
        scheduler.syncRepeating(
                task -> {
                    if (counter.incrementAndGet() >= 5) {
                        task.cancel();
                    }
                },
                0,
                10);
        CompletableFuture<String> async = scheduler.supplyAsync(() -> "done");
        assertEquals(0, counter.get());
        assertFalse(async.isDone());

        scheduler.tick();
        assertEquals("done", async.get());
        scheduler.tick(19);
        assertEquals(20, scheduler.currentTick());
        assertEquals(3, counter.get(), "repeating ran at ticks 1 and 11, the one-shot at tick 20");
        scheduler.tick(100);
        assertEquals(0, scheduler.activeTaskCount());
    }

    @Test
    void primaryThreadCanBeSimulated() {
        ManualScheduler scheduler = ManualScheduler.create();
        scheduler.ensurePrimaryThread("ok");
        scheduler.primaryThread(false);
        assertThrows(IllegalStateException.class, () -> scheduler.ensurePrimaryThread("Open menu"));
    }

    @Test
    void closeCancelsTasks() {
        ManualScheduler scheduler = ManualScheduler.create();
        AtomicInteger runs = new AtomicInteger();
        scheduler.syncRepeating(task -> runs.incrementAndGet(), 0, 1);
        scheduler.close();
        scheduler.tick(5);
        assertEquals(0, runs.get());
    }

    @Test
    void testClockDrivesCooldowns() {
        TestClock clock = TestClock.start();
        Cooldowns<UUID> cooldowns = Cooldowns.of(Duration.ofSeconds(10), clock);
        UUID id = UUID.randomUUID();
        assertTrue(cooldowns.tryUse(id));
        clock.advance(Duration.ofSeconds(9));
        assertFalse(cooldowns.tryUse(id));
        clock.advance(Duration.ofSeconds(1));
        assertTrue(cooldowns.tryUse(id));
    }
}
