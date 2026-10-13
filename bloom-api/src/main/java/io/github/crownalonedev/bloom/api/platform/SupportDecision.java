package io.github.crownalonedev.bloom.api.platform;

/**
 * Result of {@link SupportPolicy#evaluate}.
 *
 * @since 0.1.0
 */
public final class SupportDecision {

    private final PlatformFamily family;
    private final boolean untested;
    private final String reason;

    private SupportDecision(PlatformFamily family, boolean untested, String reason) {
        this.family = family;
        this.untested = untested;
        this.reason = reason;
    }

    static SupportDecision supported(PlatformFamily family, boolean untested) {
        return new SupportDecision(family, untested, null);
    }

    static SupportDecision unsupported(String reason) {
        return new SupportDecision(null, false, reason);
    }

    /**
     * Returns whether Bloom may start.
     *
     * @return {@code true} if an adapter family was selected
     */
    public boolean isSupported() {
        return family != null;
    }

    /**
     * Returns the selected adapter family.
     *
     * @return family
     * @throws IllegalStateException if unsupported
     */
    public PlatformFamily family() {
        if (family == null) {
            throw new IllegalStateException("Platform is unsupported: " + reason);
        }
        return family;
    }

    /**
     * Returns whether the platform is newer than the supported range and was only accepted because the developer
     * opted in.
     *
     * @return {@code true} for an explicitly allowed untested version
     */
    public boolean isUntested() {
        return untested;
    }

    /**
     * Returns why the platform is unsupported.
     *
     * @return explanation, or {@code null} if supported
     */
    public String reason() {
        return reason;
    }
}
