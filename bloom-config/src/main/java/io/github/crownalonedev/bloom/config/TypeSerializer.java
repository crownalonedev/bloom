package io.github.crownalonedev.bloom.config;

/**
 * Converts a custom type to and from plain YAML values (strings, numbers, booleans, lists and maps).
 *
 * <pre>{@code
 * TypeSerializer<Location> location = new TypeSerializer<Location>() {
 *     public Location deserialize(Object raw) {
 *         String[] p = raw.toString().split(",");
 *         return new Location(Bukkit.getWorld(p[0]), Double.parseDouble(p[1]), ...);
 *     }
 *     public Object serialize(Location value) {
 *         return value.getWorld().getName() + "," + value.getX() + ...;
 *     }
 * };
 * YamlConfig config = YamlConfig.builder(path).serializer(Location.class, location).build();
 * }</pre>
 *
 * @param <T> handled type
 * @since 0.1.0
 */
public interface TypeSerializer<T> {

    /**
     * Converts a YAML value to the type.
     *
     * @param raw value as loaded from YAML (never {@code null})
     * @return converted value
     * @throws IllegalArgumentException with a short reason if the value cannot be converted; Bloom adds the file,
     *     key and line to the message
     */
    T deserialize(Object raw);

    /**
     * Converts the type to a YAML value.
     *
     * @param value value (never {@code null})
     * @return a string, number, boolean, list or map
     */
    Object serialize(T value);
}
