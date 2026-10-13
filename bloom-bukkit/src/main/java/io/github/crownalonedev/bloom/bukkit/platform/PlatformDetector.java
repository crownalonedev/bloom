package io.github.crownalonedev.bloom.bukkit.platform;

import io.github.crownalonedev.bloom.api.BloomStartupException;
import io.github.crownalonedev.bloom.api.platform.JavaRuntime;
import io.github.crownalonedev.bloom.api.platform.MinecraftVersion;
import io.github.crownalonedev.bloom.api.platform.PlatformInfo;
import io.github.crownalonedev.bloom.api.platform.ServerSoftware;
import java.util.Optional;
import org.bukkit.Server;

/**
 * Detects the server implementation and Minecraft version from what the server reports.
 *
 * <p>Implementation detection uses class probes (without initialising classes) rather than the server name alone,
 * because forks often report custom names.
 *
 * @since 0.1.0
 */
public final class PlatformDetector {

    /** Probe for class presence; abstracted so detection logic is unit-testable without a server. */
    public interface ClassProbe {
        /**
         * Tests whether a class exists.
         *
         * @param className fully qualified class name
         * @return {@code true} if loadable
         */
        boolean exists(String className);
    }

    private PlatformDetector() {}

    /**
     * Detects the platform of a running server.
     *
     * @param server the server
     * @return platform description
     * @throws BloomStartupException if the Minecraft version cannot be determined
     */
    public static PlatformInfo detect(final Server server) {
        final ClassLoader loader = server.getClass().getClassLoader();
        ClassProbe probe = new ClassProbe() {
            @Override
            public boolean exists(String className) {
                try {
                    Class.forName(className, false, loader);
                    return true;
                } catch (ClassNotFoundException | LinkageError e) {
                    return false;
                }
            }
        };
        return detect(
                server.getName(),
                server.getVersion(),
                server.getBukkitVersion(),
                server.getClass().getName(),
                probe,
                JavaRuntime.current());
    }

    /**
     * Detects a platform from raw values.
     *
     * @param serverName {@code Server#getName()}
     * @param serverVersion {@code Server#getVersion()}
     * @param bukkitVersion {@code Server#getBukkitVersion()}
     * @param serverClassName runtime class name of the {@code Server} implementation
     * @param probe class presence probe
     * @param java Java runtime
     * @return platform description
     * @throws BloomStartupException if the Minecraft version cannot be determined
     */
    public static PlatformInfo detect(
            String serverName,
            String serverVersion,
            String bukkitVersion,
            String serverClassName,
            ClassProbe probe,
            JavaRuntime java) {
        Optional<MinecraftVersion> version = MinecraftVersion.detect(bukkitVersion, serverVersion);
        if (!version.isPresent()) {
            throw new BloomStartupException("Bloom could not determine the Minecraft version. Server reported name='"
                    + serverName + "', version='" + serverVersion + "', bukkitVersion='" + bukkitVersion
                    + "'. Please report this with your server jar details.");
        }
        ServerSoftware software = detectSoftware(serverName, serverClassName, probe);
        return new PlatformInfo(software, serverName, serverVersion, bukkitVersion, version.get(), java);
    }

    /**
     * Identifies the server implementation.
     *
     * @param serverName {@code Server#getName()}
     * @param serverClassName runtime class name of the server implementation
     * @param probe class presence probe
     * @return the implementation
     */
    public static ServerSoftware detectSoftware(String serverName, String serverClassName, ClassProbe probe) {
        if (serverClassName != null && serverClassName.startsWith("org.mockbukkit.")) {
            return ServerSoftware.MOCKBUKKIT;
        }
        // Server-internal class that only exists in Folia (the Folia scheduler *API* also exists in Paper).
        if (probe.exists("io.papermc.paper.threadedregions.RegionizedServer")) {
            return ServerSoftware.FOLIA;
        }
        if (probe.exists("io.papermc.paper.ServerBuildInfo")
                || probe.exists("io.papermc.paper.configuration.Configuration")
                || probe.exists("com.destroystokyo.paper.PaperConfig")) {
            return "Paper".equalsIgnoreCase(serverName) ? ServerSoftware.PAPER : ServerSoftware.PAPER_FORK;
        }
        if (probe.exists("org.github.paperspigot.PaperSpigotConfig")) {
            return ServerSoftware.PAPER_SPIGOT;
        }
        if (probe.exists("org.spigotmc.SpigotConfig")) {
            return ServerSoftware.SPIGOT;
        }
        if ("CraftBukkit".equalsIgnoreCase(serverName)) {
            return ServerSoftware.CRAFTBUKKIT;
        }
        return ServerSoftware.UNKNOWN;
    }
}
