package io.github.crownalonedev.bloomexamples.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crownalonedev.bloom.api.scheduler.BloomScheduler;
import io.github.crownalonedev.bloom.testing.ManualScheduler;
import org.junit.jupiter.api.Test;

// @snippet:testing.countdown
/** Code under test depends on BloomScheduler, not on Bukkit, so it can be tested without a server. */
final class Countdown {
    private final BloomScheduler scheduler;
    private int remaining;

    Countdown(BloomScheduler scheduler) {
        this.scheduler = scheduler;
    }

    void start(int seconds) {
        remaining = seconds;
        scheduler.syncRepeating(
                task -> {
                    if (--remaining <= 0) {
                        task.cancel();
                    }
                },
                20,
                20);
    }

    boolean finished() {
        return remaining <= 0;
    }
}

class CountdownTest {
    @Test
    void finishesAfterThreeSeconds() {
        ManualScheduler scheduler = ManualScheduler.create();
        Countdown countdown = new Countdown(scheduler);
        countdown.start(3);

        scheduler.tick(59);
        assertTrue(!countdown.finished());
        scheduler.tick();
        assertTrue(countdown.finished());
        assertEquals(0, scheduler.activeTaskCount());
    }
}
// @end
