package io.github.crownalonedev.bloom.gui;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Per-player state of one open menu. It survives refreshes, page changes and back-navigation to the same view, and
 * is discarded when the player closes the menu. Main thread only.
 *
 * @since 0.1.0
 */
public final class MenuState {

    private final Map<StateKey<?>, Object> values = new IdentityHashMap<StateKey<?>, Object>();

    MenuState() {}

    /**
     * Returns a value.
     *
     * @param key key
     * @param <T> value type
     * @return the value, or empty
     */
    public <T> Optional<T> get(StateKey<T> key) {
        return Optional.ofNullable(key.type().cast(values.get(key)));
    }

    /**
     * Returns a value or a default.
     *
     * @param key key
     * @param fallback value returned when absent
     * @param <T> value type
     * @return the value or {@code fallback}
     */
    public <T> T getOrDefault(StateKey<T> key, T fallback) {
        Object value = values.get(key);
        return value == null ? fallback : key.type().cast(value);
    }

    /**
     * Stores a value.
     *
     * @param key key
     * @param value value, or {@code null} to remove
     * @param <T> value type
     */
    public <T> void set(StateKey<T> key, T value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, key.type().cast(value));
        }
    }

    /**
     * Removes a value.
     *
     * @param key key
     */
    public void remove(StateKey<?> key) {
        values.remove(key);
    }

    /**
     * Returns whether a value is present.
     *
     * @param key key
     * @return {@code true} if present
     */
    public boolean has(StateKey<?> key) {
        return values.containsKey(key);
    }
}
