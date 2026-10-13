package io.github.crownalonedev.bloom.common.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Converts a documented subset of MiniMessage into legacy {@code §} formatted text, for platforms without
 * Adventure (1.7.10 / 1.8.x).
 *
 * <p>Supported: named colours ({@code <red>}), {@code <color:...>}/{@code <c:...>}, hex colours
 * ({@code <#ff66aa>}, downsampled to the nearest legacy colour), decorations ({@code <bold>}, {@code <b>},
 * {@code <italic>}, {@code <i>}, {@code <em>}, {@code <underlined>}, {@code <u>}, {@code <strikethrough>},
 * {@code <st>}, {@code <obfuscated>}, {@code <obf>}) including negation ({@code <!bold>}, {@code <bold:false>}),
 * closing tags, {@code <reset>}, {@code <newline>}/{@code <br>}, {@code <pre>} and {@code \<} escapes.
 *
 * <p>Degraded: {@code <gradient>} and {@code <transition>} use their first colour; {@code <rainbow>},
 * {@code <click>}, {@code <hover>}, {@code <insert>}, {@code <font>} and {@code <shadow>} keep their content but lose
 * their effect. Unknown tags are kept as literal text, as MiniMessage does.
 */
public final class MiniMessageLite {

    private static final List<String> EFFECT_ONLY_TAGS =
            Arrays.asList("rainbow", "click", "hover", "insert", "insertion", "font", "shadow");

    private MiniMessageLite() {}

    /**
     * Converts MiniMessage text to legacy formatted text.
     *
     * @param input MiniMessage text
     * @return text with {@code §} codes
     */
    public static String toLegacy(String input) {
        return new Converter(input).run();
    }

    /**
     * Escapes text so it is rendered literally when parsed as MiniMessage. Compatible with real MiniMessage.
     *
     * @param text untrusted text
     * @return escaped text
     */
    public static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<' || c == '\\') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    /** Decoration codes in a fixed order: bold, italic, underlined, strikethrough, obfuscated. */
    private static final char[] DECORATION_CODES = {'l', 'o', 'n', 'm', 'k'};

    private static int decorationIndex(String name) {
        switch (name) {
            case "bold":
            case "b":
                return 0;
            case "italic":
            case "i":
            case "em":
                return 1;
            case "underlined":
            case "u":
                return 2;
            case "strikethrough":
            case "st":
                return 3;
            case "obfuscated":
            case "obf":
                return 4;
            default:
                return -1;
        }
    }

    private static String canonical(String name) {
        int decoration = decorationIndex(name);
        if (decoration >= 0) {
            return "decoration" + decoration;
        }
        if (name.equals("c") || name.equals("colour")) {
            return "color";
        }
        if (name.equals("br")) {
            return "newline";
        }
        if (name.equals("insertion")) {
            return "insert";
        }
        return name;
    }

    private static final class Style {
        final LegacyColor color;
        final boolean[] decorations;

        Style(LegacyColor color, boolean[] decorations) {
            this.color = color;
            this.decorations = decorations;
        }

        static Style plain() {
            return new Style(null, new boolean[5]);
        }

        Style withColor(LegacyColor newColor) {
            return new Style(newColor, decorations.clone());
        }

        Style withDecoration(int index, boolean value) {
            boolean[] copy = decorations.clone();
            copy[index] = value;
            return new Style(color, copy);
        }

        boolean sameAs(Style other) {
            return color == other.color && Arrays.equals(decorations, other.decorations);
        }
    }

    private static final class Frame {
        final String tag;
        final Style style;

        Frame(String tag, Style style) {
            this.tag = tag;
            this.style = style;
        }
    }

    private static final class Converter {
        private final String in;
        private final StringBuilder out = new StringBuilder();
        private final List<Frame> stack = new ArrayList<Frame>();
        private Style emitted = Style.plain();

        Converter(String in) {
            this.in = in;
        }

        Style current() {
            return stack.isEmpty() ? Style.plain() : stack.get(stack.size() - 1).style;
        }

        String run() {
            int i = 0;
            int length = in.length();
            while (i < length) {
                char c = in.charAt(i);
                if (c == '\\' && i + 1 < length && (in.charAt(i + 1) == '<' || in.charAt(i + 1) == '\\')) {
                    appendText(in.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if (c == '<') {
                    int end = findTagEnd(i + 1);
                    if (end > 0 && handleTag(in.substring(i + 1, end))) {
                        i = end + 1;
                        if (inPre) {
                            i = consumePre(i);
                        }
                        continue;
                    }
                }
                appendText(c);
                i++;
            }
            return out.toString();
        }

        private boolean inPre;

        private int consumePre(int from) {
            int close = in.indexOf("</pre>", from);
            String literal = close < 0 ? in.substring(from) : in.substring(from, close);
            for (int k = 0; k < literal.length(); k++) {
                appendText(literal.charAt(k));
            }
            inPre = false;
            return close < 0 ? in.length() : close + "</pre>".length();
        }

        private int findTagEnd(int from) {
            char quote = 0;
            for (int j = from; j < in.length(); j++) {
                char c = in.charAt(j);
                if (quote != 0) {
                    if (c == '\\' && j + 1 < in.length()) {
                        j++;
                    } else if (c == quote) {
                        quote = 0;
                    }
                } else if (c == '\'' || c == '"') {
                    quote = c;
                } else if (c == '>') {
                    return j;
                } else if (c == '<') {
                    return -1;
                }
            }
            return -1;
        }

        private boolean handleTag(String content) {
            if (content.isEmpty()) {
                return false;
            }
            if (content.charAt(0) == '/') {
                return close(content.substring(1).split(":", 2)[0].toLowerCase(Locale.ROOT));
            }
            boolean negated = content.charAt(0) == '!';
            String body = negated ? content.substring(1) : content;
            String[] parts = body.split(":", 2);
            String name = parts[0].toLowerCase(Locale.ROOT);
            String args = parts.length > 1 ? parts[1] : null;

            int decoration = decorationIndex(name);
            if (decoration >= 0) {
                boolean value = !negated && !"false".equalsIgnoreCase(args);
                push(name, current().withDecoration(decoration, value));
                return true;
            }
            if (negated) {
                return false;
            }
            if (name.startsWith("#")) {
                LegacyColor hex = parseColor(name);
                if (hex == null) {
                    return false;
                }
                push(name, current().withColor(hex));
                return true;
            }
            LegacyColor named = LegacyColor.byName(name);
            if (named != null) {
                push(name, current().withColor(named));
                return true;
            }
            switch (canonical(name)) {
                case "color": {
                    LegacyColor color = args == null ? null : parseColor(args);
                    if (color == null) {
                        return false;
                    }
                    push(name, current().withColor(color));
                    return true;
                }
                case "gradient":
                case "transition": {
                    LegacyColor first = null;
                    if (args != null) {
                        for (String arg : args.split(":")) {
                            first = parseColor(arg);
                            if (first != null) {
                                break;
                            }
                        }
                    }
                    push(name, first == null ? current() : current().withColor(first));
                    return true;
                }
                case "reset":
                    stack.clear();
                    return true;
                case "newline":
                    appendText('\n');
                    return true;
                case "pre":
                    inPre = true;
                    return true;
                default:
                    if (EFFECT_ONLY_TAGS.contains(name)) {
                        push(name, current());
                        return true;
                    }
                    return false;
            }
        }

        private LegacyColor parseColor(String raw) {
            String value = raw.trim().toLowerCase(Locale.ROOT);
            if (value.startsWith("#") && value.length() == 7) {
                try {
                    return LegacyColor.nearest(Integer.parseInt(value.substring(1), 16));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return LegacyColor.byName(value);
        }

        private void push(String tag, Style style) {
            stack.add(new Frame(canonical(tag), style));
        }

        private boolean close(String rawName) {
            String name = canonical(rawName);
            boolean colorClose = LegacyColor.byName(rawName) != null || rawName.startsWith("#");
            for (int k = stack.size() - 1; k >= 0; k--) {
                String tag = stack.get(k).tag;
                boolean matches = tag.equals(name)
                        || (colorClose
                                && (LegacyColor.byName(tag) != null || tag.startsWith("#") || tag.equals("color")));
                if (matches) {
                    while (stack.size() > k) {
                        stack.remove(stack.size() - 1);
                    }
                    return true;
                }
            }
            return false;
        }

        private void appendText(char c) {
            Style desired = current();
            if (!desired.sameAs(emitted)) {
                emit(desired);
            }
            out.append(c);
        }

        private void emit(Style desired) {
            boolean additive = desired.color == emitted.color;
            if (additive) {
                for (int k = 0; k < 5; k++) {
                    if (emitted.decorations[k] && !desired.decorations[k]) {
                        additive = false;
                        break;
                    }
                }
            }
            if (additive) {
                for (int k = 0; k < 5; k++) {
                    if (desired.decorations[k] && !emitted.decorations[k]) {
                        out.append(LegacyText.SECTION).append(DECORATION_CODES[k]);
                    }
                }
            } else {
                out.append(LegacyText.SECTION).append(desired.color == null ? 'r' : desired.color.code());
                for (int k = 0; k < 5; k++) {
                    if (desired.decorations[k]) {
                        out.append(LegacyText.SECTION).append(DECORATION_CODES[k]);
                    }
                }
            }
            emitted = desired;
        }
    }
}
