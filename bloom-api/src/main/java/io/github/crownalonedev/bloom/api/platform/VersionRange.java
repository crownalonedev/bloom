package io.github.crownalonedev.bloom.api.platform;

/**
 * An inclusive range of Minecraft versions.
 *
 * @since 0.1.0
 */
public final class VersionRange {

    private final MinecraftVersion min;
    private final MinecraftVersion max;

    private VersionRange(MinecraftVersion min, MinecraftVersion max) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Range minimum " + min + " is greater than maximum " + max);
        }
        this.min = min;
        this.max = max;
    }

    /**
     * Creates an inclusive range.
     *
     * @param min inclusive lower bound
     * @param max inclusive upper bound
     * @return the range
     */
    public static VersionRange between(MinecraftVersion min, MinecraftVersion max) {
        return new VersionRange(min, max);
    }

    /**
     * Creates a range covering every patch release of one release line from a minimum patch,
     * e.g. {@code lineFrom(26, 3, 0)} covers 26.3, 26.3.1, 26.3.2 and so on.
     *
     * @param major major component
     * @param minor minor component
     * @param minPatch first included patch
     * @return the range
     */
    public static VersionRange lineFrom(int major, int minor, int minPatch) {
        return new VersionRange(
                MinecraftVersion.of(major, minor, minPatch), MinecraftVersion.of(major, minor, Integer.MAX_VALUE));
    }

    /**
     * Returns the inclusive lower bound.
     *
     * @return lower bound
     */
    public MinecraftVersion min() {
        return min;
    }

    /**
     * Returns the inclusive upper bound.
     *
     * @return upper bound; its patch is {@link Integer#MAX_VALUE} for open release lines
     */
    public MinecraftVersion max() {
        return max;
    }

    /**
     * Tests whether a version lies in this range.
     *
     * @param version version to test
     * @return {@code true} if included
     */
    public boolean contains(MinecraftVersion version) {
        return version.isBetween(min, max);
    }

    @Override
    public String toString() {
        String upper = max.patch() == Integer.MAX_VALUE ? max.major() + "." + max.minor() + ".x" : max.toString();
        return min.equals(max) ? min.toString() : min + " - " + upper;
    }
}
