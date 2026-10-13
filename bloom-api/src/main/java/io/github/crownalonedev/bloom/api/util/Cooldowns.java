package io.github.crownalonedev.bloom.api.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cooldown tracking keyed by any value, typically a player UUID.
 *
 * <pre>{@code
 * private final Cooldowns<UUID> pearl = Cooldowns.of(Duration.ofSeconds(15));
 *
 * if (!pearl.tryUse(player.getUniqueId())) {
 *     player.sendMessage("Wait " + Durations.format(pearl.remaining(player.getUniqueId())));
 *     return;
 * }
 * }</pre>
 *
 * <p>Expired entries are removed lazily when accessed and by {@link #prune()}. Cooldowns are kept in memory only.
 *
 * @param <K> key type; must have stable {@code equals}/{@code hashCode}
 * @since 0.1.0
 */
public final class Cooldowns<K> {

    private final Duration defaultDuration;
    private final Clock clock;
    private final Map<K, Instant> expiries = new ConcurrentHashMap<K, Instant>();

    private Cooldowns(Duration defaultDuration, Clock clock) {
        if (defaultDuration.isNegative() || defaultDuration.isZero()) {
            throw new IllegalArgumentException("Cooldown duration must be positive: " + defaultDuration);
        }
        this.defaultDuration = defaultDuration;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Creates cooldowns with a default duration using the system clock.
     *
     * @param defaultDuration positive duration applied by {@link #tryUse(Object)} and {@link #start(Object)}
     * @param <K> key type
     * @return a new cooldown tracker
     */
    public static <K> Cooldowns<K> of(Duration defaultDuration) {
        return new Cooldowns<K>(defaultDuration, Clock.systemUTC());
    }

    /**
     * Creates cooldowns with a custom clock, mainly for tests.
     *
     * @param defaultDuration positive default duration
     * @param clock time source
     * @param <K> key type
     * @return a new cooldown tracker
     */
    public static <K> Cooldowns<K> of(Duration defaultDuration, Clock clock) {
        return new Cooldowns<K>(defaultDuration, clock);
    }

    /**
     * Starts the default cooldown if the key is not on cooldown.
     *
     * @param key key to use
     * @return {@code true} if the key was free and the cooldown was started; {@code false} if still cooling down
     */
    public boolean tryUse(K key) {
        return tryUse(key, defaultDuration);
    }

    /**
     * Starts a cooldown of a specific duration if the key is not on cooldown. Atomic per key.
     *
     * @param key key to use
     * @param duration positive cooldown duration
     * @return {@code true} if the cooldown was started
     */
    public boolean tryUse(K key, Duration duration) {
        final Instant now = clock.instant();
        final Instant next = now.plus(duration);
        final boolean[] started = {false};
        expiries.compute(key, (k, current) -> {
            if (current == null || !current.isAfter(now)) {
                started[0] = true;
                return next;
            }
            return current;
        });
        return started[0];
    }

    /**
     * Starts (or restarts) the default cooldown unconditionally.
     *
     * @param key key
     */
    public void start(K key) {
        start(key, defaultDuration);
    }

    /**
     * Starts (or restarts) a cooldown unconditionally.
     *
     * @param key key
     * @param duration positive duration
     */
    public void start(K key, Duration duration) {
        expiries.put(key, clock.instant().plus(duration));
    }

    /**
     * Returns whether a key is cooling down.
     *
     * @param key key
     * @return {@code true} if the cooldown has not expired
     */
    public boolean isActive(K key) {
        return remaining(key).compareTo(Duration.ZERO) > 0;
    }

    /**
     * Returns the remaining cooldown time.
     *
     * @param key key
     * @return remaining time, {@link Duration#ZERO} if not on cooldown
     */
    public Duration remaining(K key) {
        Instant expiry = expiries.get(key);
        if (expiry == null) {
            return Duration.ZERO;
        }
        Instant now = clock.instant();
        if (!expiry.isAfter(now)) {
            expiries.remove(key, expiry);
            return Duration.ZERO;
        }
        return Duration.between(now, expiry);
    }

    /**
     * Returns when the key's cooldown expires.
     *
     * @param key key
     * @return expiry instant, or empty if not on cooldown
     */
    public Optional<Instant> expiry(K key) {
        return isActive(key) ? Optional.ofNullable(expiries.get(key)) : Optional.<Instant>empty();
    }

    /**
     * Clears a key's cooldown.
     *
     * @param key key
     */
    public void reset(K key) {
        expiries.remove(key);
    }

    /** Clears all cooldowns. */
    public void clear() {
        expiries.clear();
    }

    /**
     * Removes expired entries. Call periodically if keys are rarely accessed again.
     *
     * @return number of removed entries
     */
    public int prune() {
        Instant now = clock.instant();
        int removed = 0;
        for (Iterator<Map.Entry<K, Instant>> it = expiries.entrySet().iterator(); it.hasNext(); ) {
            if (!it.next().getValue().isAfter(now)) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * Returns the number of tracked entries, including expired entries not yet pruned.
     *
     * @return entry count
     */
    public int size() {
        return expiries.size();
    }

    /**
     * Returns the default duration.
     *
     * @return default duration
     */
    public Duration defaultDuration() {
        return defaultDuration;
    }
}
