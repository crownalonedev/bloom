package io.github.crownalonedev.bloom.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crownalonedev.bloom.api.scheduler.Ticks;
import io.github.crownalonedev.bloom.api.text.Placeholder;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UtilitiesTest {

    /** A clock that only moves when told to. */
    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void cooldownLifecycle() {
        MutableClock clock = new MutableClock();
        Cooldowns<UUID> cooldowns = Cooldowns.of(Duration.ofSeconds(10), clock);
        UUID id = UUID.randomUUID();

        assertTrue(cooldowns.tryUse(id));
        assertFalse(cooldowns.tryUse(id));
        assertEquals(Duration.ofSeconds(10), cooldowns.remaining(id));

        clock.advance(Duration.ofSeconds(4));
        assertEquals(Duration.ofSeconds(6), cooldowns.remaining(id));
        assertTrue(cooldowns.isActive(id));

        clock.advance(Duration.ofSeconds(6));
        assertFalse(cooldowns.isActive(id));
        assertTrue(cooldowns.tryUse(id));

        cooldowns.reset(id);
        assertFalse(cooldowns.isActive(id));
    }

    @Test
    void cooldownPruneRemovesExpired() {
        MutableClock clock = new MutableClock();
        Cooldowns<String> cooldowns = Cooldowns.of(Duration.ofSeconds(1), clock);
        cooldowns.start("a");
        cooldowns.start("b", Duration.ofMinutes(1));
        clock.advance(Duration.ofSeconds(2));
        assertEquals(1, cooldowns.prune());
        assertEquals(1, cooldowns.size());
    }

    @Test
    void durationParsing() {
        assertEquals(Duration.ofSeconds(90), Durations.parse("90"));
        assertEquals(Duration.ofMinutes(90), Durations.parse("1h30m"));
        assertEquals(Duration.ofHours(52), Durations.parse("2d 4h"));
        assertEquals(Duration.ofMillis(500), Durations.parse("500ms"));
        assertEquals(Duration.ofDays(14), Durations.parse("2W"));
        assertThrows(IllegalArgumentException.class, () -> Durations.parse("5 minutes"));
        assertThrows(IllegalArgumentException.class, () -> Durations.parse("x5s"));
        assertThrows(IllegalArgumentException.class, () -> Durations.parse(""));
    }

    @Test
    void durationFormatting() {
        assertEquals("1h 5m 3s", Durations.format(Duration.ofSeconds(3903)));
        assertEquals("0s", Durations.format(Duration.ZERO));
        assertEquals("<1s", Durations.format(Duration.ofMillis(10)));
        assertEquals("1 hour, 5 minutes and 3 seconds", Durations.formatLong(Duration.ofSeconds(3903)));
        assertEquals("2 days and 1 second", Durations.formatLong(Duration.ofSeconds(2 * 86400 + 1)));
        assertEquals("1 minute", Durations.formatLong(Duration.ofMinutes(1)));
    }

    @Test
    void ticksRoundUp() {
        assertEquals(1, Ticks.from(Duration.ofMillis(1)));
        assertEquals(20, Ticks.from(Duration.ofSeconds(1)));
        assertEquals(0, Ticks.from(Duration.ZERO));
        assertEquals(Duration.ofSeconds(2), Ticks.toDuration(40));
    }

    @Test
    void expiringCacheHonoursTtlAndSize() {
        MutableClock clock = new MutableClock();
        ExpiringCache<String, Integer> cache = ExpiringCache.create(Duration.ofSeconds(5), 2, clock);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.get("a");
        cache.put("c", 3); // evicts least recently used: b
        assertFalse(cache.get("b").isPresent());
        assertEquals(Integer.valueOf(1), cache.get("a").get());
        clock.advance(Duration.ofSeconds(6));
        assertFalse(cache.get("a").isPresent());
        assertEquals(Integer.valueOf(42), cache.get("z", k -> 42));
    }

    @Test
    void placeholderValidation() {
        assertThrows(IllegalArgumentException.class, () -> Placeholder.of("", 1));
        assertThrows(IllegalArgumentException.class, () -> Placeholder.of("{x}", 1));
        assertEquals("null", Placeholder.of("x", null).value());
    }
}
