package io.github.crownalonedev.bloom.common.text;

/** Utilities for legacy {@code §}/{@code &} formatted strings. Behaviour matches Bukkit's ChatColor helpers. */
public final class LegacyText {

    /** The section sign used by the client for formatting codes. */
    public static final char SECTION = '§';

    private static final String CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    private LegacyText() {}

    /**
     * Translates {@code &} codes into {@code §} codes. Also translates {@code &#RRGGBB} hex colours: into the
     * {@code §x§R§R§G§G§B§B} form when {@code hexSupported}, otherwise into the nearest legacy colour.
     *
     * @param text text with {@code &} codes
     * @param hexSupported whether the target platform renders RGB colours
     * @return text with {@code §} codes
     */
    public static String colorize(String text, boolean hexSupported) {
        StringBuilder out = new StringBuilder(text.length());
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < length) {
                char next = text.charAt(i + 1);
                if (next == '#' && i + 8 <= length && isHex(text, i + 2, i + 8)) {
                    appendHex(out, text.substring(i + 2, i + 8), hexSupported);
                    i += 7;
                    continue;
                }
                if (CODES.indexOf(next) >= 0 && next != 'x' && next != 'X') {
                    out.append(SECTION).append(Character.toLowerCase(next));
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * Appends a hex colour in the form the platform understands.
     *
     * @param out destination
     * @param hex six hex digits
     * @param hexSupported whether RGB is supported
     */
    static void appendHex(StringBuilder out, String hex, boolean hexSupported) {
        if (hexSupported) {
            out.append(SECTION).append('x');
            for (int i = 0; i < 6; i++) {
                out.append(SECTION).append(Character.toLowerCase(hex.charAt(i)));
            }
        } else {
            out.append(SECTION)
                    .append(LegacyColor.nearest(Integer.parseInt(hex, 16)).code());
        }
    }

    /**
     * Removes all {@code §} formatting codes, including {@code §x} hex sequences.
     *
     * @param text formatted text
     * @return plain text
     */
    public static String strip(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == SECTION && i + 1 < text.length()) {
                i++;
                continue;
            }
            if (c != SECTION) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Removes formatting codes from untrusted text so it cannot apply formatting, while keeping {@code &}
     * characters readable.
     *
     * @param text untrusted text
     * @return text without {@code §} codes
     */
    public static String neutralize(String text) {
        return text.indexOf(SECTION) < 0 ? text : strip(text);
    }

    /**
     * Truncates formatted text to a maximum number of characters without leaving a dangling {@code §}.
     *
     * @param text formatted text
     * @param maxLength maximum length in chars
     * @return possibly shortened text
     */
    public static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        String cut = text.substring(0, maxLength);
        if (cut.charAt(cut.length() - 1) == SECTION) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut;
    }

    private static boolean isHex(String text, int from, int to) {
        for (int i = from; i < to; i++) {
            if (Character.digit(text.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
