package io.github.crownalonedev.bloom.config;

import io.github.crownalonedev.bloom.config.internal.ConfigPaths;
import java.util.Map;

/**
 * Mutable access to configuration values during a {@link ConfigMigration}. Paths are dot-separated.
 *
 * @since 0.1.0
 */
public final class MigrationContext {

    private final Map<String, Object> values;
    private final int fromVersion;

    MigrationContext(Map<String, Object> values, int fromVersion) {
        this.values = values;
        this.fromVersion = fromVersion;
    }

    /**
     * Returns the version being migrated from.
     *
     * @return source version; the migration produces {@code fromVersion() + 1}
     */
    public int fromVersion() {
        return fromVersion;
    }

    /**
     * Returns whether a value exists.
     *
     * @param path path
     * @return {@code true} if present
     */
    public boolean has(String path) {
        return ConfigPaths.get(values, path) != null;
    }

    /**
     * Returns a raw value (string, number, boolean, list or map).
     *
     * @param path path
     * @return value, or {@code null}
     */
    public Object get(String path) {
        return ConfigPaths.get(values, path);
    }

    /**
     * Sets a raw value; {@code null} removes it.
     *
     * @param path path
     * @param value value
     */
    public void set(String path, Object value) {
        ConfigPaths.set(values, path, ConfigPaths.deepCopy(value));
    }

    /**
     * Removes a value.
     *
     * @param path path
     */
    public void remove(String path) {
        ConfigPaths.set(values, path, null);
    }

    /**
     * Moves a value (including whole sections). Does nothing if the source is missing; replaces the target.
     *
     * @param from source path
     * @param to target path
     */
    public void move(String from, String to) {
        Object value = ConfigPaths.get(values, from);
        if (value == null) {
            return;
        }
        ConfigPaths.set(values, from, null);
        ConfigPaths.set(values, to, value);
    }
}
