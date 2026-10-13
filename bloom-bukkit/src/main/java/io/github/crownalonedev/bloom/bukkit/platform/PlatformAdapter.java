package io.github.crownalonedev.bloom.bukkit.platform;

import io.github.crownalonedev.bloom.api.platform.Capabilities;
import io.github.crownalonedev.bloom.api.platform.PlatformFamily;

/**
 * A version adapter: the only place where version-specific Bukkit/Paper/NMS behaviour lives.
 *
 * <p><strong>SPI:</strong> implemented by {@code bloom-legacy} and {@code bloom-modern}; not intended for plugin
 * code. Implementations must have a public no-argument constructor and must not touch server state in it. Bloom
 * loads an adapter class by name only after {@link io.github.crownalonedev.bloom.api.platform.SupportPolicy}
 * selected its family, so classes of other adapters are never loaded.
 *
 * @since 0.1.0
 */
public interface PlatformAdapter {

    /**
     * Returns the family this adapter serves.
     *
     * @return platform family
     */
    PlatformFamily family();

    /**
     * Returns a short description for logs, e.g. {@code "legacy (Bukkit 1.7.10-1.8.9)"}.
     *
     * @return description
     */
    String description();

    /**
     * Initialises the adapter and probes optional features. Called once on the main thread.
     *
     * @param context startup context
     * @return capabilities available on this server
     */
    Capabilities enable(AdapterContext context);

    /** Releases adapter resources. Called once during Bloom shutdown, on the main thread. */
    void disable();

    /**
     * Returns text rendering and delivery.
     *
     * @return text platform
     */
    TextPlatform text();

    /**
     * Returns player and inventory operations.
     *
     * @return player platform
     */
    PlayerPlatform players();

    /**
     * Returns item operations.
     *
     * @return item platform
     */
    ItemPlatform items();
}
