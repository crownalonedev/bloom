package io.github.crownalonedev.bloom.api.platform;

/**
 * Server implementations Bloom can recognise.
 *
 * <p>Recognition does not imply support; see {@link PlatformFamily} and the compatibility matrix.
 *
 * @since 0.1.0
 */
public enum ServerSoftware {
    /** CraftBukkit without Spigot patches. */
    CRAFTBUKKIT("CraftBukkit", false),
    /** Spigot. */
    SPIGOT("Spigot", false),
    /** PaperSpigot, the legacy (1.7/1.8) Paper fork of Spigot. */
    PAPER_SPIGOT("PaperSpigot", false),
    /** Paper (1.9 and newer). */
    PAPER("Paper", true),
    /** A Paper fork such as Purpur or Pufferfish; Paper API is available. */
    PAPER_FORK("Paper fork", true),
    /** Folia, Paper's regionised fork. Not supported by Bloom yet: its scheduler model differs. */
    FOLIA("Folia", true),
    /** MockBukkit test server (implements Paper API). */
    MOCKBUKKIT("MockBukkit", true),
    /** Unrecognised implementation. */
    UNKNOWN("Unknown", false);

    private final String displayName;
    private final boolean paperApi;

    ServerSoftware(String displayName, boolean paperApi) {
        this.displayName = displayName;
        this.paperApi = paperApi;
    }

    /**
     * Returns a human-readable name.
     *
     * @return display name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns whether this implementation exposes the modern Paper API (Adventure, PDC, etc.).
     *
     * @return {@code true} for Paper, Paper forks, Folia and MockBukkit
     */
    public boolean hasPaperApi() {
        return paperApi;
    }
}
