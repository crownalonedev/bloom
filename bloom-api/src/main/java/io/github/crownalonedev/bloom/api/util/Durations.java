package io.github.crownalonedev.bloom.api.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsing and formatting of human-friendly durations such as {@code "1h30m"} or {@code "2d 4h"}.
 *
 * <p>Supported units: {@code ms}, {@code s}, {@code m}, {@code h}, {@code d}, {@code w}. Months and years are not
 * supported because their length varies.
 *
 * @since 0.1.0
 */
public final class Durations {

    private static final Pattern PART = Pattern.compile("(\\d+)\\s*(ms|s|m|h|d|w)", Pattern.CASE_INSENSITIVE);

    private static final long[] UNIT_SECONDS = {604_800L, 86_400L, 3_600L, 60L, 1L};
    private static final String[] SHORT_UNITS = {"w", "d", "h", "m", "s"};
    private static final String[] LONG_UNITS = {"week", "day", "hour", "minute", "second"};

    private Durations() {}

    /**
     * Parses a duration such as {@code "90s"}, {@code "1h30m"}, {@code "2d 4h"} or {@code "500ms"}. A bare number
     * is interpreted as seconds.
     *
     * @param text text to parse
     * @return the duration
     * @throws IllegalArgumentException if the text is not a valid duration, with a message showing valid examples
     */
    public static Duration parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Duration is empty. Examples: 30s, 5m, 1h30m, 2d");
        }
        String input = text.trim().toLowerCase(Locale.ROOT);
        if (input.matches("\\d+")) {
            return Duration.ofSeconds(Long.parseLong(input));
        }
        Matcher matcher = PART.matcher(input);
        Duration total = Duration.ZERO;
        int position = 0;
        while (matcher.find()) {
            if (!input.substring(position, matcher.start()).trim().isEmpty()) {
                break;
            }
            long amount = Long.parseLong(matcher.group(1));
            total = total.plus(unit(amount, matcher.group(2).toLowerCase(Locale.ROOT)));
            position = matcher.end();
        }
        if (position == 0 || !input.substring(position).trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid duration '" + text + "'. Examples: 30s, 5m, 1h30m, 2d 4h, "
                    + "500ms. Units: ms, s, m, h, d, w");
        }
        return total;
    }

    private static Duration unit(long amount, String unit) {
        switch (unit) {
            case "ms":
                return Duration.ofMillis(amount);
            case "s":
                return Duration.ofSeconds(amount);
            case "m":
                return Duration.ofMinutes(amount);
            case "h":
                return Duration.ofHours(amount);
            case "d":
                return Duration.ofDays(amount);
            case "w":
                return Duration.ofDays(amount * 7);
            default:
                throw new IllegalArgumentException("Unknown unit " + unit);
        }
    }

    /**
     * Formats a duration compactly, e.g. {@code "1h 5m 3s"}. Sub-second durations above zero format as
     * {@code "<1s"}; zero formats as {@code "0s"}.
     *
     * @param duration non-negative duration
     * @return compact representation
     */
    public static String format(Duration duration) {
        return format(duration, false);
    }

    /**
     * Formats a duration in words, e.g. {@code "1 hour, 5 minutes and 3 seconds"}.
     *
     * @param duration non-negative duration
     * @return long-form English representation
     */
    public static String formatLong(Duration duration) {
        return format(duration, true);
    }

    private static String format(Duration duration, boolean words) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Duration must not be negative: " + duration);
        }
        long seconds = duration.getSeconds();
        if (seconds == 0) {
            if (duration.isZero()) {
                return words ? "0 seconds" : "0s";
            }
            return words ? "less than a second" : "<1s";
        }
        StringBuilder out = new StringBuilder();
        int written = 0;
        int partCount = 0;
        long remaining = seconds;
        for (long unitSeconds : UNIT_SECONDS) {
            if (remaining >= unitSeconds) {
                partCount++;
                remaining %= unitSeconds;
            }
        }
        remaining = seconds;
        for (int i = 0; i < UNIT_SECONDS.length; i++) {
            long value = remaining / UNIT_SECONDS[i];
            if (value == 0) {
                continue;
            }
            remaining %= UNIT_SECONDS[i];
            if (written > 0) {
                if (!words) {
                    out.append(' ');
                } else if (written == partCount - 1) {
                    out.append(" and ");
                } else {
                    out.append(", ");
                }
            }
            if (words) {
                out.append(value).append(' ').append(LONG_UNITS[i]).append(value == 1 ? "" : "s");
            } else {
                out.append(value).append(SHORT_UNITS[i]);
            }
            written++;
        }
        return out.toString();
    }
}
