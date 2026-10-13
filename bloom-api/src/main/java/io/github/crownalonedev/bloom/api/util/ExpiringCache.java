package io.github.crownalonedev.bloom.api.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A small thread-safe cache with time-to-live expiry and a maximum size (least-recently-used eviction).
 *
 * <p>Intended for modest caches such as recently looked-up player data. It uses a single lock, so it is not
 * suitable for very hot, highly contended paths; for those use a dedicated caching library.
 *
 * @param <K> key type
 * @param <V> value type
 * @since 0.1.0
 */
public final class ExpiringCache<K, V> {

    private final Duration ttl;
    private final int maxSize;
    private final Clock clock;
    private final LinkedHashMap<K, CacheEntry<V>> map;

    private ExpiringCache(Duration ttl, int maxSize, Clock clock) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("TTL must be positive: " + ttl);
        }
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize must be at least 1: " + maxSize);
        }
        this.ttl = ttl;
        this.maxSize = maxSize;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.map = new LinkedHashMap<K, CacheEntry<V>>(16, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > ExpiringCache.this.maxSize;
            }
        };
    }

    /**
     * Creates a cache.
     *
     * @param ttl how long entries stay valid after being written
     * @param maxSize maximum number of entries
     * @param <K> key type
     * @param <V> value type
     * @return the cache
     */
    public static <K, V> ExpiringCache<K, V> create(Duration ttl, int maxSize) {
        return new ExpiringCache<K, V>(ttl, maxSize, Clock.systemUTC());
    }

    /**
     * Creates a cache with a custom clock, mainly for tests.
     *
     * @param ttl entry lifetime
     * @param maxSize maximum number of entries
     * @param clock time source
     * @param <K> key type
     * @param <V> value type
     * @return the cache
     */
    public static <K, V> ExpiringCache<K, V> create(Duration ttl, int maxSize, Clock clock) {
        return new ExpiringCache<K, V>(ttl, maxSize, clock);
    }

    /**
     * Returns a cached value if present and not expired.
     *
     * @param key key
     * @return value, or empty
     */
    public synchronized Optional<V> get(K key) {
        CacheEntry<V> entry = map.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt.isAfter(clock.instant())) {
            map.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(entry.value);
    }

    /**
     * Returns the cached value, computing and caching it when absent or expired. The loader runs while the cache
     * lock is held, so it must be fast and must not access this cache.
     *
     * @param key key
     * @param loader computes a missing value; must not return {@code null}
     * @return cached or computed value
     */
    public synchronized V get(K key, Function<? super K, ? extends V> loader) {
        Optional<V> cached = get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        V value = Objects.requireNonNull(loader.apply(key), "loader returned null");
        put(key, value);
        return value;
    }

    /**
     * Stores a value.
     *
     * @param key key
     * @param value value, not {@code null}
     */
    public synchronized void put(K key, V value) {
        map.put(
                key,
                new CacheEntry<V>(
                        Objects.requireNonNull(value, "value"), clock.instant().plus(ttl)));
    }

    /**
     * Removes a value.
     *
     * @param key key
     */
    public synchronized void invalidate(K key) {
        map.remove(key);
    }

    /** Removes all values. */
    public synchronized void clear() {
        map.clear();
    }

    /**
     * Removes expired entries.
     *
     * @return number of removed entries
     */
    public synchronized int prune() {
        Instant now = clock.instant();
        int removed = 0;
        for (Iterator<CacheEntry<V>> it = map.values().iterator(); it.hasNext(); ) {
            if (!it.next().expiresAt.isAfter(now)) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * Returns the number of entries, including expired entries not yet pruned.
     *
     * @return size
     */
    public synchronized int size() {
        return map.size();
    }

    private static final class CacheEntry<V> {
        final V value;
        final Instant expiresAt;

        CacheEntry(V value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }
}
