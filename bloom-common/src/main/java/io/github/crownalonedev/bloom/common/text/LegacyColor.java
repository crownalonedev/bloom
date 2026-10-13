package io.github.crownalonedev.bloom.common.text;

import java.util.Locale;

/** The 16 legacy chat colours with their codes, MiniMessage names and RGB values. */
public enum LegacyColor {
    BLACK('0', "black", 0x000000),
    DARK_BLUE('1', "dark_blue", 0x0000AA),
    DARK_GREEN('2', "dark_green", 0x00AA00),
    DARK_AQUA('3', "dark_aqua", 0x00AAAA),
    DARK_RED('4', "dark_red", 0xAA0000),
    DARK_PURPLE('5', "dark_purple", 0xAA00AA),
    GOLD('6', "gold", 0xFFAA00),
    GRAY('7', "gray", 0xAAAAAA),
    DARK_GRAY('8', "dark_gray", 0x555555),
    BLUE('9', "blue", 0x5555FF),
    GREEN('a', "green", 0x55FF55),
    AQUA('b', "aqua", 0x55FFFF),
    RED('c', "red", 0xFF5555),
    LIGHT_PURPLE('d', "light_purple", 0xFF55FF),
    YELLOW('e', "yellow", 0xFFFF55),
    WHITE('f', "white", 0xFFFFFF);

    private final char code;
    private final String miniName;
    private final int rgb;

    LegacyColor(char code, String miniName, int rgb) {
        this.code = code;
        this.miniName = miniName;
        this.rgb = rgb;
    }

    /**
     * Returns the legacy code character.
     *
     * @return {@code '0'}-{@code 'f'}
     */
    public char code() {
        return code;
    }

    /**
     * Returns the MiniMessage colour name.
     *
     * @return e.g. {@code "light_purple"}
     */
    public String miniName() {
        return miniName;
    }

    /**
     * Returns the RGB value.
     *
     * @return {@code 0xRRGGBB}
     */
    public int rgb() {
        return rgb;
    }

    /**
     * Looks up a colour by MiniMessage name; {@code grey} spellings are accepted.
     *
     * @param name colour name, case-insensitive
     * @return the colour, or {@code null}
     */
    public static LegacyColor byName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).replace("grey", "gray");
        for (LegacyColor color : values()) {
            if (color.miniName.equals(normalized)) {
                return color;
            }
        }
        return null;
    }

    /**
     * Finds the closest legacy colour to an RGB value using weighted Euclidean distance. Noticeably saturated
     * colours are only matched against chromatic legacy colours, so pastel tones such as {@code #F2A7CC} map to
     * pink rather than gray.
     *
     * @param rgb {@code 0xRRGGBB}
     * @return nearest legacy colour
     */
    public static LegacyColor nearest(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        boolean chromatic = max > 0 && (max - min) / (double) max >= 0.25;
        LegacyColor best = WHITE;
        double bestDistance = Double.MAX_VALUE;
        for (LegacyColor color : values()) {
            if (chromatic && color.isNeutral()) {
                continue;
            }
            int cr = (color.rgb >> 16) & 0xFF;
            int cg = (color.rgb >> 8) & 0xFF;
            int cb = color.rgb & 0xFF;
            double distance = 0.30 * sq(r - cr) + 0.59 * sq(g - cg) + 0.11 * sq(b - cb);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = color;
            }
        }
        return best;
    }

    private boolean isNeutral() {
        return this == BLACK || this == DARK_GRAY || this == GRAY || this == WHITE;
    }

    private static double sq(int v) {
        return (double) v * v;
    }
}
