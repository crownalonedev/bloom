package io.github.crownalonedev.bloom.api.platform;

/**
 * Immutable description of the server Bloom is running on.
 *
 * @since 0.1.0
 */
public final class PlatformInfo {

    private final ServerSoftware software;
    private final String serverName;
    private final String serverVersion;
    private final String bukkitVersion;
    private final MinecraftVersion minecraftVersion;
    private final JavaRuntime javaRuntime;

    /**
     * Creates a platform description.
     *
     * @param software detected implementation
     * @param serverName value of {@code Server#getName()}
     * @param serverVersion value of {@code Server#getVersion()}
     * @param bukkitVersion value of {@code Server#getBukkitVersion()}
     * @param minecraftVersion detected game version
     * @param javaRuntime running Java runtime
     */
    public PlatformInfo(
            ServerSoftware software,
            String serverName,
            String serverVersion,
            String bukkitVersion,
            MinecraftVersion minecraftVersion,
            JavaRuntime javaRuntime) {
        this.software = software;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.bukkitVersion = bukkitVersion;
        this.minecraftVersion = minecraftVersion;
        this.javaRuntime = javaRuntime;
    }

    /**
     * Returns the detected server implementation.
     *
     * @return server software
     */
    public ServerSoftware software() {
        return software;
    }

    /**
     * Returns the name the server reports, e.g. {@code "Paper"} or {@code "CraftBukkit"}.
     *
     * @return server name
     */
    public String serverName() {
        return serverName;
    }

    /**
     * Returns the raw server version string.
     *
     * @return e.g. {@code "git-Spigot-db6de12-18fbb24 (MC: 1.8.8)"}
     */
    public String serverVersion() {
        return serverVersion;
    }

    /**
     * Returns the raw Bukkit API version string.
     *
     * @return e.g. {@code "1.8.8-R0.1-SNAPSHOT"}
     */
    public String bukkitVersion() {
        return bukkitVersion;
    }

    /**
     * Returns the Minecraft version.
     *
     * @return game version
     */
    public MinecraftVersion minecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Returns the Java runtime.
     *
     * @return Java runtime
     */
    public JavaRuntime javaRuntime() {
        return javaRuntime;
    }

    /**
     * Returns a short description such as {@code "Paper 1.21.11 (Java 21.0.11)"}.
     *
     * @return description for logs and error messages
     */
    public String describe() {
        return software.displayName() + " " + minecraftVersion + " (Java " + javaRuntime.fullVersion() + ")";
    }

    @Override
    public String toString() {
        return describe();
    }
}
