package io.github.crownalonedev.bloom.bukkit.platform;

import io.github.crownalonedev.bloom.api.platform.PlatformInfo;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

/**
 * Information passed to {@link PlatformAdapter#enable(AdapterContext)}.
 *
 * @since 0.1.0
 */
public final class AdapterContext {

    private final Plugin plugin;
    private final PlatformInfo platform;
    private final Logger logger;
    private final boolean debug;

    /**
     * Creates a context.
     *
     * @param plugin owning plugin
     * @param platform detected platform
     * @param logger plugin logger
     * @param debug whether verbose diagnostics are enabled
     */
    public AdapterContext(Plugin plugin, PlatformInfo platform, Logger logger, boolean debug) {
        this.plugin = plugin;
        this.platform = platform;
        this.logger = logger;
        this.debug = debug;
    }

    /**
     * Returns the owning plugin.
     *
     * @return plugin
     */
    public Plugin plugin() {
        return plugin;
    }

    /**
     * Returns the detected platform.
     *
     * @return platform
     */
    public PlatformInfo platform() {
        return platform;
    }

    /**
     * Returns the plugin logger.
     *
     * @return logger
     */
    public Logger logger() {
        return logger;
    }

    /**
     * Returns whether debug diagnostics are enabled.
     *
     * @return debug flag
     */
    public boolean debug() {
        return debug;
    }
}
