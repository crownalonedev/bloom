package io.github.crownalonedev.bloom.config;

import io.github.crownalonedev.bloom.config.internal.ConfigPaths;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Typed, path-based access to a section of a {@link YamlConfig}. Paths are dot-separated ({@code "database.host"})
 * and relative to this section.
 *
 * <h2>Defaults</h2>
 * A value missing from the file falls back to the bundled defaults. Required getters ({@code getString(path)}) throw
 * {@link ConfigException} when the value is missing in both; getters with a fallback argument return the fallback.
 *
 * <h2>Errors</h2>
 * Type mismatches throw {@link ConfigException} naming the file, path, line and the expected type, e.g.
 * {@code config.yml line 7: 'limits.homes' expected a whole number but found 'ten'}.
 *
 * <p>All methods are thread-safe.
 *
 * @since 0.1.0
 */
public class ConfigSection {

    private final YamlConfig rootConfig;
    private final String prefix;

    /**
     * Creates a section view.
     *
     * @param root owning config, or {@code null} when this section is the config itself
     * @param prefix path of this section
     */
    ConfigSection(YamlConfig root, String prefix) {
        this.rootConfig = root;
        this.prefix = prefix;
    }

    private YamlConfig root() {
        return rootConfig != null ? rootConfig : (YamlConfig) this;
    }

    /**
     * Returns this section's path from the root.
     *
     * @return path, empty for the root
     */
    public String path() {
        return prefix;
    }

    String absolute(String path) {
        return ConfigPaths.join(prefix, path);
    }

    /**
     * Returns whether a value exists in the file or the defaults.
     *
     * @param path relative path
     * @return {@code true} if present
     */
    public boolean has(String path) {
        return root().raw(absolute(path)) != null;
    }

    /**
     * Returns whether a value exists in the file itself (ignoring defaults).
     *
     * @param path relative path
     * @return {@code true} if set in the file
     */
    public boolean isSet(String path) {
        return root().rawFromFile(absolute(path)) != null;
    }

    /**
     * Returns the keys of this section. If the section exists in the file, only the file's keys are returned (a
     * section a user trimmed is not refilled from defaults); otherwise the defaults' keys.
     *
     * @return keys in file order
     */
    @SuppressWarnings("unchecked")
    public Set<String> keys() {
        Object section = prefix.isEmpty() ? root().fileValues() : root().rawFromFile(prefix);
        if (!(section instanceof Map)) {
            section = prefix.isEmpty() ? root().defaultValues() : root().rawFromDefaults(prefix);
        }
        if (!(section instanceof Map)) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<String>(((Map<String, ?>) section).keySet()));
    }

    /**
     * Returns a child section.
     *
     * @param path relative path
     * @return the section
     * @throws ConfigException if the path holds a value that is not a section, or is missing
     */
    public ConfigSection section(String path) {
        Object raw = root().raw(absolute(path));
        if (!(raw instanceof Map)) {
            throw root().error(absolute(path), raw == null ? "is missing (expected a section)" : "expected a section");
        }
        return new ConfigSection(root(), absolute(path));
    }

    /**
     * Returns a child section if present.
     *
     * @param path relative path
     * @return the section, or empty
     */
    public Optional<ConfigSection> optionalSection(String path) {
        return root().raw(absolute(path)) instanceof Map
                ? Optional.of(new ConfigSection(root(), absolute(path)))
                : Optional.<ConfigSection>empty();
    }

    /**
     * Returns a required string.
     *
     * @param path relative path
     * @return value
     */
    public String getString(String path) {
        return get(path, String.class);
    }

    /**
     * Returns a string or a fallback.
     *
     * @param path relative path
     * @param fallback returned when missing
     * @return value
     */
    public String getString(String path, String fallback) {
        return get(path, String.class, fallback);
    }

    /**
     * Returns a required int.
     *
     * @param path relative path
     * @return value
     */
    public int getInt(String path) {
        return get(path, Integer.class);
    }

    /**
     * Returns an int or a fallback.
     *
     * @param path relative path
     * @param fallback returned when missing
     * @return value
     */
    public int getInt(String path, int fallback) {
        return get(path, Integer.class, fallback);
    }

    /**
     * Returns a required long.
     *
     * @param path relative path
     * @return value
     */
    public long getLong(String path) {
        return get(path, Long.class);
    }

    /**
     * Returns a long or a fallback.
     *
     * @param path relative path
     * @param fallback returned when missing
     * @return value
     */
    public long getLong(String path, long fallback) {
        return get(path, Long.class, fallback);
    }

    /**
     * Returns a required double.
     *
     * @param path relative path
     * @return value
     */
    public double getDouble(String path) {
        return get(path, Double.class);
    }

    /**
     * Returns a double or a fallback.
     *
     * @param path relative path
     * @param fallback returned when missing
     * @return value
     */
    public double getDouble(String path, double fallback) {
        return get(path, Double.class, fallback);
    }

    /**
     * Returns a required boolean ({@code true/false}, also {@code yes/no}, {@code on/off}).
     *
     * @param path relative path
     * @return value
     */
    public boolean getBoolean(String path) {
        return get(path, Boolean.class);
    }

    /**
     * Returns a boolean or a fallback.
     *
     * @param path relative path
     * @param fallback returned when missing
     * @return value
     */
    public boolean getBoolean(String path, boolean fallback) {
        return get(path, Boolean.class, fallback);
    }

    /**
     * Returns a required duration written like {@code "30s"}, {@code "1h30m"}, or a number of seconds.
     *
     * @param path relative path
     * @return value
     */
    public Duration getDuration(String path) {
        return get(path, Duration.class);
    }

    /**
     * Returns a duration or a fallback.
     *
     * @param path relative path
     * @param fallback returned when missing
     * @return value
     */
    public Duration getDuration(String path, Duration fallback) {
        return get(path, Duration.class, fallback);
    }

    /**
     * Returns a required enum constant (case-insensitive; {@code -} and spaces match {@code _}).
     *
     * @param path relative path
     * @param type enum class
     * @param <E> enum type
     * @return value
     */
    public <E extends Enum<E>> E getEnum(String path, Class<E> type) {
        return get(path, type);
    }

    /**
     * Returns a required list of strings. A single scalar is treated as a one-element list.
     *
     * @param path relative path
     * @return unmodifiable list
     */
    public List<String> getStringList(String path) {
        return getList(path, String.class);
    }

    /**
     * Returns a required list with typed elements.
     *
     * @param path relative path
     * @param elementType element type
     * @param <T> element type
     * @return unmodifiable list
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String path, Class<T> elementType) {
        Object raw = require(path);
        if (!(raw instanceof List) && !(raw instanceof Map)) {
            raw = Collections.singletonList(raw);
        }
        List<T> list = (List<T>) convert(path, raw, listOf(elementType));
        return Collections.unmodifiableList(new ArrayList<T>(list));
    }

    /**
     * Returns a required value converted to any supported type: scalars, enums, {@code UUID}, {@code Duration},
     * types with a registered {@link TypeSerializer}, and POJOs with a no-argument constructor (fields mapped from
     * kebab-case keys; missing keys keep field defaults).
     *
     * @param path relative path
     * @param type target type
     * @param <T> type
     * @return value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String path, Class<T> type) {
        return (T) convert(path, require(path), type);
    }

    /**
     * Returns a value converted to a type, or a fallback when missing. Present but invalid values still throw.
     *
     * @param path relative path
     * @param type target type
     * @param fallback returned when missing
     * @param <T> type
     * @return value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String path, Class<T> type, T fallback) {
        Object raw = root().raw(absolute(path));
        return raw == null ? fallback : (T) convert(path, raw, type);
    }

    /**
     * Maps this whole section to a POJO (see {@link #get(String, Class)}).
     *
     * @param type POJO type
     * @param <T> type
     * @return mapped object
     */
    @SuppressWarnings("unchecked")
    public <T> T as(Class<T> type) {
        Object raw = prefix.isEmpty() ? root().mergedRoot() : root().raw(prefix);
        return (T) convert("", raw, type);
    }

    /**
     * Sets a value in memory (call {@link YamlConfig#save()} to write it). Values are serialised with the
     * registered serializers; {@code null} removes the key.
     *
     * @param path relative path
     * @param value value
     */
    public void set(String path, Object value) {
        root().setValue(absolute(path), value);
    }

    /**
     * Removes a value in memory.
     *
     * @param path relative path
     */
    public void remove(String path) {
        root().setValue(absolute(path), null);
    }

    private Object require(String path) {
        Object raw = root().raw(absolute(path));
        if (raw == null) {
            throw root().error(absolute(path), "is missing and has no default value");
        }
        return raw;
    }

    private Object convert(String path, Object raw, Type type) {
        String absolute = path.isEmpty() ? prefix : absolute(path);
        try {
            return root().mapper().deserialize(raw, type);
        } catch (IllegalArgumentException e) {
            throw root().error(absolute, e.getMessage());
        }
    }

    private static Type listOf(final Class<?> element) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {element};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }
}
