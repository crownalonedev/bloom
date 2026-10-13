package io.github.crownalonedev.bloom.api.scheduler;

import java.time.Duration;

/**
 * Conversions between server ticks and {@link Duration}s. One tick is 50 ms at the nominal 20 TPS.
 *
 * @since 0.1.0
 */
public final class Ticks {

    /** Nominal ticks per second. */
    public static final int PER_SECOND = 20;

    /** Nominal milliseconds per tick. */
    public static final long MILLIS_PER_TICK = 50L;

    private Ticks() {}

    /**
     * Converts a duration to ticks, rounding up so that a positive duration never becomes zero ticks.
     *
     * @param duration non-negative duration
     * @return number of ticks
     * @throws IllegalArgumentException if the duration is negative
     */
    public static long from(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Duration must not be negative: " + duration);
        }
        long millis = duration.toMillis();
        return (millis + MILLIS_PER_TICK - 1) / MILLIS_PER_TICK;
    }

    /**
     * Converts ticks to a duration.
     *
     * @param ticks number of ticks
     * @return the nominal duration
     */
    public static Duration toDuration(long ticks) {
        return Duration.ofMillis(ticks * MILLIS_PER_TICK);
    }

    /**
     * Converts seconds to ticks.
     *
     * @param seconds seconds
     * @return ticks
     */
    public static long seconds(long seconds) {
        return seconds * PER_SECOND;
    }
}
