package io.github.crownalonedev.bloom.api.text;

import java.util.Objects;

/**
 * A {@code {key}} placeholder and the value that replaces it.
 *
 * <p>Values created with {@link #of(String, Object)} are inserted as <em>plain text</em>: formatting codes or tags
 * inside the value are neutralised, so untrusted input such as player chat cannot inject colours or MiniMessage
 * tags. Use {@link #formatted(String, String)} only for trusted values that intentionally contain formatting.
 *
 * <pre>{@code
 * messenger.send(player, "&dWelcome, {player}!", Placeholder.of("player", player.getName()));
 * }</pre>
 *
 * @since 0.1.0
 */
public final class Placeholder {

    private final String key;
    private final String value;
    private final boolean formatted;

    private Placeholder(String key, String value, boolean formatted) {
        this.key = Objects.requireNonNull(key, "key");
        if (key.isEmpty() || key.indexOf('{') >= 0 || key.indexOf('}') >= 0) {
            throw new IllegalArgumentException(
                    "Placeholder key must be non-empty and must not contain braces: '" + key + "'");
        }
        this.value = value;
        this.formatted = formatted;
    }

    /**
     * Creates a plain-text placeholder. The value's {@code toString()} is used; {@code null} becomes
     * {@code "null"}.
     *
     * @param key key without braces, e.g. {@code "player"}
     * @param value replacement value
     * @return the placeholder
     */
    public static Placeholder of(String key, Object value) {
        return new Placeholder(key, String.valueOf(value), false);
    }

    /**
     * Creates a placeholder whose value is parsed with the message's format. Only use with trusted values.
     *
     * @param key key without braces
     * @param value replacement containing formatting in the same {@link TextFormat} as the message
     * @return the placeholder
     */
    public static Placeholder formatted(String key, String value) {
        return new Placeholder(key, Objects.requireNonNull(value, "value"), true);
    }

    /**
     * Returns the key without braces.
     *
     * @return key
     */
    public String key() {
        return key;
    }

    /**
     * Returns the replacement value.
     *
     * @return value
     */
    public String value() {
        return value;
    }

    /**
     * Returns whether the value is trusted formatted text rather than plain text.
     *
     * @return {@code true} if created with {@link #formatted(String, String)}
     */
    public boolean isFormatted() {
        return formatted;
    }

    @Override
    public String toString() {
        return "{" + key + "}=" + value;
    }
}
