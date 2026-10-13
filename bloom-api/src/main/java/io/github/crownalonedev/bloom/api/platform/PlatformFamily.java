package io.github.crownalonedev.bloom.api.platform;

/**
 * The platform families Bloom ships adapters for. Each family is served by one adapter module.
 *
 * @since 0.1.0
 */
public enum PlatformFamily {
    /** Bukkit/Spigot 1.7.10 to 1.8.9 on Java 8. Served by {@code bloom-legacy}. */
    LEGACY("legacy", "bloom-legacy"),
    /** Paper 1.21.x and 26.x on Java 21+. Served by {@code bloom-modern}. */
    MODERN("modern", "bloom-modern");

    private final String id;
    private final String adapterModule;

    PlatformFamily(String id, String adapterModule) {
        this.id = id;
        this.adapterModule = adapterModule;
    }

    /**
     * Returns the short identifier used in logs and configuration.
     *
     * @return {@code "legacy"} or {@code "modern"}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the Bloom artifact that contains this family's adapter.
     *
     * @return artifact id, e.g. {@code "bloom-legacy"}
     */
    public String adapterModule() {
        return adapterModule;
    }
}
