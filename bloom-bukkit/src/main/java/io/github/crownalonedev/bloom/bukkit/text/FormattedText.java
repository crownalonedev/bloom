package io.github.crownalonedev.bloom.bukkit.text;

import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.api.text.TextFormat;
import io.github.crownalonedev.bloom.common.text.TextTemplates;
import java.util.Objects;

/**
 * Text whose placeholders have been applied and which is ready for an adapter to render.
 *
 * <p>For {@link TextFormat#MINIMESSAGE}, {@link #content()} is MiniMessage markup. For {@link TextFormat#LEGACY},
 * it is a {@code §}-formatted string ({@code &} codes already translated). Plugin code normally never creates this
 * directly; {@link Messenger} and the item/menu builders do.
 *
 * @since 0.1.0
 */
public final class FormattedText {

    private final String content;
    private final TextFormat format;

    private FormattedText(String content, TextFormat format) {
        this.content = Objects.requireNonNull(content, "content");
        this.format = Objects.requireNonNull(format, "format");
    }

    /**
     * Applies placeholders to a template.
     *
     * @param template message template in {@code format}
     * @param format template format
     * @param hexSupported whether {@code &#RRGGBB} may produce real RGB colours
     * @param placeholders placeholder values
     * @return prepared text
     */
    public static FormattedText prepare(
            String template, TextFormat format, boolean hexSupported, Placeholder... placeholders) {
        return new FormattedText(TextTemplates.prepare(template, format, hexSupported, placeholders), format);
    }

    /**
     * Wraps already-prepared content.
     *
     * @param content prepared content (MiniMessage markup or {@code §} string)
     * @param format format of {@code content}
     * @return text
     */
    public static FormattedText ofPrepared(String content, TextFormat format) {
        return new FormattedText(content, format);
    }

    /**
     * Returns the prepared content.
     *
     * @return MiniMessage markup or a {@code §}-formatted string, depending on {@link #format()}
     */
    public String content() {
        return content;
    }

    /**
     * Returns the content format.
     *
     * @return format
     */
    public TextFormat format() {
        return format;
    }

    /**
     * Returns whether the content is empty.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    @Override
    public String toString() {
        return format + ":" + content;
    }
}
