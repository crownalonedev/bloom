package io.github.crownalonedev.bloom.gui;

import java.util.Objects;

/**
 * A typed key for per-player menu state.
 *
 * <pre>{@code
 * static final StateKey<Integer> QUANTITY = StateKey.of("quantity", Integer.class);
 *
 * int quantity = view.state().getOrDefault(QUANTITY, 1);
 * view.state().set(QUANTITY, quantity + 1);
 * }</pre>
 *
 * @param <T> value type
 * @since 0.1.0
 */
public final class StateKey<T> {

    private final String name;
    private final Class<T> type;

    private StateKey(String name, Class<T> type) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Creates a key. Keys are compared by identity, so declare them as constants.
     *
     * @param name descriptive name used in {@code toString()}
     * @param type value type
     * @param <T> value type
     * @return a new key
     */
    public static <T> StateKey<T> of(String name, Class<T> type) {
        return new StateKey<T>(name, type);
    }

    /**
     * Returns the key name.
     *
     * @return name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the value type.
     *
     * @return type
     */
    public Class<T> type() {
        return type;
    }

    @Override
    public String toString() {
        return "StateKey[" + name + ":" + type.getSimpleName() + "]";
    }
}
