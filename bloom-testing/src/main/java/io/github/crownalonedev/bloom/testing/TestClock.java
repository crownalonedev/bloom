package io.github.crownalonedev.bloom.testing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A {@link Clock} that only moves when told to. Pass it to {@code Cooldowns.of(duration, clock)} or
 * {@code ExpiringCache.create(ttl, size, clock)} to test time-based logic without sleeping.
 *
 * <pre>{@code
 * TestClock clock = TestClock.start();
 * Cooldowns<UUID> cooldowns = Cooldowns.of(Duration.ofSeconds(10), clock);
 * cooldowns.tryUse(id);
 * clock.advance(Duration.ofSeconds(10));
 * assertTrue(cooldowns.tryUse(id));
 * }</pre>
 *
 * @since 0.1.0
 */
public final class TestClock extends Clock {

    private Instant now;

    private TestClock(Instant start) {
        this.now = start;
    }

    /**
     * Creates a clock at 2026-01-01T00:00:00Z.
     *
     * @return clock
     */
    public static TestClock start() {
        return new TestClock(Instant.parse("2026-01-01T00:00:00Z"));
    }

    /**
     * Creates a clock at a given instant.
     *
     * @param instant start time
     * @return clock
     */
    public static TestClock at(Instant instant) {
        return new TestClock(instant);
    }

    /**
     * Moves time forward.
     *
     * @param duration amount, may be negative to move backwards
     */
    public synchronized void advance(Duration duration) {
        now = now.plus(duration);
    }

    /**
     * Sets the time.
     *
     * @param instant new time
     */
    public synchronized void set(Instant instant) {
        now = instant;
    }

    @Override
    public synchronized Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
