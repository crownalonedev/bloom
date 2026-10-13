package io.github.crownalonedev.bloom.api.platform;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An immutable Minecraft release version such as {@code 1.8.8}, {@code 1.21.11} or {@code 26.3}.
 *
 * <p>Both the classic {@code 1.x.y} scheme and Mojang's year-based scheme ({@code 26.1}, {@code 26.3}) are
 * represented as {@code major.minor.patch}. Because {@code 1 < 26}, natural ordering is correct across schemes.
 * Pre-release suffixes ({@code -pre3}, {@code -rc1}) are ignored when parsing.
 *
 * @since 0.1.0
 */
public final class MinecraftVersion implements Comparable<MinecraftVersion> {

    /** Minecraft 1.7.10, the only supported 1.7 release. */
    public static final MinecraftVersion V1_7_10 = of(1, 7, 10);
    /** Minecraft 1.8 (1.8.0). */
    public static final MinecraftVersion V1_8 = of(1, 8, 0);
    /** Minecraft 1.8.8, the most common legacy PvP version. */
    public static final MinecraftVersion V1_8_8 = of(1, 8, 8);
    /** Minecraft 1.8.9, the last 1.8 release. */
    public static final MinecraftVersion V1_8_9 = of(1, 8, 9);
    /** Minecraft 1.21.1. */
    public static final MinecraftVersion V1_21_1 = of(1, 21, 1);
    /** Minecraft 1.21.11, the last 1.x release. */
    public static final MinecraftVersion V1_21_11 = of(1, 21, 11);
    /** Minecraft 26.1, the first year-based release. */
    public static final MinecraftVersion V26_1 = of(26, 1, 0);

    private static final Pattern MC_MARKER = Pattern.compile("\\(MC: (\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final Pattern LEADING = Pattern.compile("^\\s*(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private final int major;
    private final int minor;
    private final int patch;

    private MinecraftVersion(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException(
                    "Version components must be non-negative: " + major + "." + minor + "." + patch);
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Creates a version.
     *
     * @param major major component, {@code 1} for classic versions or the year for year-based versions
     * @param minor minor component
     * @param patch patch component
     * @return the version
     */
    public static MinecraftVersion of(int major, int minor, int patch) {
        return new MinecraftVersion(major, minor, patch);
    }

    /**
     * Creates a version with patch {@code 0}.
     *
     * @param major major component
     * @param minor minor component
     * @return the version
     */
    public static MinecraftVersion of(int major, int minor) {
        return new MinecraftVersion(major, minor, 0);
    }

    /**
     * Parses a version from the start of a string such as {@code "1.8.8"}, {@code "1.21.11-R0.1-SNAPSHOT"} or
     * {@code "26.3.build.177-stable"}.
     *
     * @param text text beginning with a version
     * @return the parsed version
     * @throws IllegalArgumentException if the text does not start with a version
     */
    public static MinecraftVersion parse(String text) {
        Optional<MinecraftVersion> parsed = tryParse(text);
        if (!parsed.isPresent()) {
            throw new IllegalArgumentException("Not a Minecraft version: '" + text + "'");
        }
        return parsed.get();
    }

    /**
     * Parses a version from the start of a string, returning empty instead of throwing.
     *
     * @param text text beginning with a version, may be {@code null}
     * @return the parsed version, or empty
     */
    public static Optional<MinecraftVersion> tryParse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        return fromMatcher(LEADING.matcher(text));
    }

    /**
     * Determines the Minecraft version from the strings a Bukkit-family server reports.
     *
     * <p>The {@code "(MC: x.y.z)"} marker in {@code Server#getVersion()} is preferred because it always contains
     * the game version; {@code Server#getBukkitVersion()} is used as a fallback.
     *
     * @param bukkitVersion value of {@code Server#getBukkitVersion()}, may be {@code null}
     * @param serverVersion value of {@code Server#getVersion()}, may be {@code null}
     * @return the detected version, or empty if neither string contains one
     */
    public static Optional<MinecraftVersion> detect(String bukkitVersion, String serverVersion) {
        if (serverVersion != null) {
            Optional<MinecraftVersion> marked = fromMatcher(MC_MARKER.matcher(serverVersion));
            if (marked.isPresent()) {
                return marked;
            }
        }
        return tryParse(bukkitVersion);
    }

    private static Optional<MinecraftVersion> fromMatcher(Matcher matcher) {
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
            return Optional.of(new MinecraftVersion(major, minor, patch));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the major component.
     *
     * @return major component
     */
    public int major() {
        return major;
    }

    /**
     * Returns the minor component.
     *
     * @return minor component
     */
    public int minor() {
        return minor;
    }

    /**
     * Returns the patch component.
     *
     * @return patch component, {@code 0} if the version string had none
     */
    public int patch() {
        return patch;
    }

    /**
     * Returns whether this version is the same as or newer than another.
     *
     * @param other version to compare with
     * @return {@code true} if {@code this >= other}
     */
    public boolean isAtLeast(MinecraftVersion other) {
        return compareTo(other) >= 0;
    }

    /**
     * Returns whether this version is older than another.
     *
     * @param other version to compare with
     * @return {@code true} if {@code this < other}
     */
    public boolean isBefore(MinecraftVersion other) {
        return compareTo(other) < 0;
    }

    /**
     * Returns whether this version lies within an inclusive range.
     *
     * @param min inclusive lower bound
     * @param max inclusive upper bound
     * @return {@code true} if {@code min <= this <= max}
     */
    public boolean isBetween(MinecraftVersion min, MinecraftVersion max) {
        return isAtLeast(min) && compareTo(max) <= 0;
    }

    /**
     * Returns whether this version shares major and minor components with another, e.g. 1.8.3 and 1.8.8.
     *
     * @param other version to compare with
     * @return {@code true} if both belong to the same release line
     */
    public boolean isSameLine(MinecraftVersion other) {
        return major == other.major && minor == other.minor;
    }

    @Override
    public int compareTo(MinecraftVersion o) {
        if (major != o.major) {
            return Integer.compare(major, o.major);
        }
        if (minor != o.minor) {
            return Integer.compare(minor, o.minor);
        }
        return Integer.compare(patch, o.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MinecraftVersion)) {
            return false;
        }
        MinecraftVersion that = (MinecraftVersion) o;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        return (major * 31 + minor) * 31 + patch;
    }

    /**
     * Formats the version the way Mojang does: {@code 1.8} rather than {@code 1.8.0}.
     *
     * @return the version string
     */
    @Override
    public String toString() {
        return patch == 0 ? major + "." + minor : major + "." + minor + "." + patch;
    }
}
