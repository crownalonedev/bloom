package io.github.crownalonedev.bloom.api.platform;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The single source of truth for which platforms Bloom will start on.
 *
 * <p>Runtime support ranges are deliberately conservative. Being inside a range means an adapter exists and its
 * code was compile-verified against that API; the test status of each version is tracked separately in
 * {@code docs/architecture/COMPATIBILITY.md}.
 *
 * @since 0.1.0
 */
public final class SupportPolicy {

    /** Legacy family: 1.7.10 through 1.8.9. */
    public static final VersionRange LEGACY = VersionRange.between(MinecraftVersion.V1_7_10, MinecraftVersion.V1_8_9);

    /** Modern family ranges: Paper 1.21.1 - 1.21.11, then 26.1.x - 26.3.x. */
    public static final List<VersionRange> MODERN = Collections.unmodifiableList(Arrays.asList(
            VersionRange.between(MinecraftVersion.V1_21_1, MinecraftVersion.V1_21_11),
            VersionRange.between(MinecraftVersion.V26_1, MinecraftVersion.of(26, 3, Integer.MAX_VALUE))));

    /** Minimum Java feature version for the legacy family. */
    public static final int LEGACY_MIN_JAVA = 8;

    /** Minimum Java feature version for the modern family (the modern adapter is Java 21 bytecode). */
    public static final int MODERN_MIN_JAVA = 21;

    private SupportPolicy() {}

    /**
     * Returns the newest Minecraft version covered by a modern range.
     *
     * @return newest supported modern version (patch may be {@link Integer#MAX_VALUE})
     */
    public static MinecraftVersion newestModern() {
        return MODERN.get(MODERN.size() - 1).max();
    }

    /**
     * Decides whether and how Bloom can run on a platform.
     *
     * @param software detected server implementation
     * @param version detected Minecraft version
     * @param java running Java runtime
     * @param allowNewerUntested whether the developer opted into running the modern adapter on Paper versions
     *     newer than the newest supported one
     * @return the decision, never {@code null}
     */
    public static SupportDecision evaluate(
            ServerSoftware software, MinecraftVersion version, JavaRuntime java, boolean allowNewerUntested) {
        if (software == ServerSoftware.FOLIA) {
            return SupportDecision.unsupported("Folia is not supported yet: its region-based threading model needs "
                    + "a dedicated scheduler adapter. Run the plugin on Paper instead.");
        }
        if (LEGACY.contains(version)) {
            if (software.hasPaperApi()) {
                return SupportDecision.unsupported("Unexpected Paper API on legacy Minecraft " + version + ".");
            }
            if (!java.isAtLeast(LEGACY_MIN_JAVA)) {
                return SupportDecision.unsupported("Java " + java.featureVersion() + " is too old; legacy servers "
                        + "need Java " + LEGACY_MIN_JAVA + "+.");
            }
            return SupportDecision.supported(PlatformFamily.LEGACY, false);
        }
        if (version.isSameLine(MinecraftVersion.of(1, 7)) || version.isSameLine(MinecraftVersion.V1_8)) {
            return SupportDecision.unsupported("Minecraft " + version + " is outside the legacy range " + LEGACY
                    + ". Only 1.7.10 and 1.8.x are supported in the legacy family.");
        }
        boolean modernRange = false;
        for (VersionRange range : MODERN) {
            if (range.contains(version)) {
                modernRange = true;
                break;
            }
        }
        boolean newer = version.compareTo(newestModern()) > 0;
        if (modernRange || (newer && allowNewerUntested)) {
            if (!software.hasPaperApi()) {
                return SupportDecision.unsupported(software.displayName() + " " + version + " is not supported: "
                        + "the modern adapter requires Paper (or a Paper fork) because it uses Adventure and "
                        + "Paper-only APIs. Spigot/CraftBukkit support for modern versions is not implemented.");
            }
            if (!java.isAtLeast(MODERN_MIN_JAVA)) {
                return SupportDecision.unsupported("Java " + java.featureVersion() + " is too old; the modern "
                        + "adapter needs Java " + MODERN_MIN_JAVA + "+.");
            }
            return SupportDecision.supported(PlatformFamily.MODERN, !modernRange);
        }
        if (newer) {
            return SupportDecision.unsupported("Minecraft " + version + " is newer than the newest version this "
                    + "Bloom release supports (" + MODERN.get(MODERN.size() - 1) + "). Update Bloom, or opt in "
                    + "with BloomOptions.allowNewerUntestedVersions(true) at your own risk.");
        }
        return SupportDecision.unsupported("Minecraft " + version + " is not supported. Supported: legacy " + LEGACY
                + "; modern " + MODERN + ". Versions 1.9 - 1.21.0 have no adapter yet.");
    }
}
