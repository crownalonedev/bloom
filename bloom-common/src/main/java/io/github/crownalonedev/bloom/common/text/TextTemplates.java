package io.github.crownalonedev.bloom.common.text;

import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.api.text.TextFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies {@code {key}} placeholders to message templates safely.
 *
 * <p>Substitution is a single pass, so a value containing {@code {other}} is never expanded again. Plain
 * placeholder values are neutralised for the template's format (MiniMessage tags escaped, section signs removed).
 */
public final class TextTemplates {

    private TextTemplates() {}

    /**
     * Prepares a template for rendering.
     *
     * <ul>
     *   <li>{@link TextFormat#MINIMESSAGE}: returns MiniMessage text with placeholders substituted.
     *   <li>{@link TextFormat#LEGACY}: returns {@code §} formatted text; {@code &} codes in the template and in
     *       formatted placeholder values are translated.
     * </ul>
     *
     * @param template message template
     * @param format template format
     * @param hexSupported whether {@code &#RRGGBB} should produce real hex colours (legacy format only)
     * @param placeholders placeholder values
     * @return prepared text in MiniMessage or legacy section format
     */
    public static String prepare(
            String template, TextFormat format, boolean hexSupported, Placeholder... placeholders) {
        if (format == TextFormat.LEGACY) {
            String colored = LegacyText.colorize(template, hexSupported);
            return substitute(colored, placeholders, format, hexSupported);
        }
        return substitute(template, placeholders, format, hexSupported);
    }

    private static String substitute(String text, Placeholder[] placeholders, TextFormat format, boolean hex) {
        if (placeholders == null || placeholders.length == 0 || text.indexOf('{') < 0) {
            return text;
        }
        Map<String, Placeholder> byKey = new HashMap<String, Placeholder>();
        for (Placeholder placeholder : placeholders) {
            byKey.put(placeholder.key(), placeholder);
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '{') {
                int close = text.indexOf('}', i + 1);
                if (close > i + 1) {
                    Placeholder placeholder = byKey.get(text.substring(i + 1, close));
                    if (placeholder != null) {
                        out.append(render(placeholder, format, hex));
                        i = close + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static String render(Placeholder placeholder, TextFormat format, boolean hex) {
        String value = placeholder.value();
        if (format == TextFormat.MINIMESSAGE) {
            // Section signs too: clients apply § codes inside components, so untrusted text could still format.
            return placeholder.isFormatted() ? value : MiniMessageLite.escape(LegacyText.neutralize(value));
        }
        return placeholder.isFormatted() ? LegacyText.colorize(value, hex) : LegacyText.neutralize(value);
    }
}
