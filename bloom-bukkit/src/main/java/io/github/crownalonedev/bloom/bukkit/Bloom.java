package io.github.crownalonedev.bloom.bukkit;

import io.github.crownalonedev.bloom.api.BloomStartupException;
import io.github.crownalonedev.bloom.api.BloomVersion;
import io.github.crownalonedev.bloom.api.lifecycle.LifecycleState;
import io.github.crownalonedev.bloom.api.platform.Capabilities;
import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.api.platform.PlatformInfo;
import io.github.crownalonedev.bloom.api.platform.SupportDecision;
import io.github.crownalonedev.bloom.api.platform.SupportPolicy;
import io.github.crownalonedev.bloom.api.scheduler.BloomScheduler;
import io.github.crownalonedev.bloom.api.service.ServiceRegistry;
import io.github.crownalonedev.bloom.bukkit.event.Events;
import io.github.crownalonedev.bloom.bukkit.internal.BukkitSchedulerBackend;
import io.github.crownalonedev.bloom.bukkit.platform.AdapterContext;
import io.github.crownalonedev.bloom.bukkit.platform.AdapterLoader;
import io.github.crownalonedev.bloom.bukkit.platform.PlatformAdapter;
import io.github.crownalonedev.bloom.bukkit.platform.PlatformDetector;
import io.github.crownalonedev.bloom.bukkit.text.Messenger;
import io.github.crownalonedev.bloom.common.lifecycle.CompositeTerminable;
import io.github.crownalonedev.bloom.common.scheduler.TrackingScheduler;
import io.github.crownalonedev.bloom.common.service.DefaultServiceRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

/**
 * The Bloom entry point for Bukkit-family plugins.
 *
 * <pre>{@code
 * public final class MyPlugin extends JavaPlugin {
 *     private Bloom bloom;
 *
 *     @Override
 *     public void onEnable() {
 *         bloom = Bloom.enable(this);
 *         bloom.messages().send(getServer().getConsoleSender(), "&dBloom is ready on {platform}",
 *                 Placeholder.of("platform", bloom.platform().describe()));
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         if (bloom != null) {
 *             bloom.shutdown();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>{@link #enable(Plugin)} detects the server, selects the matching version adapter, starts core services and
 * logs a startup banner. If the server is not supported it throws {@link BloomStartupException} with an explanation;
 * let it propagate from {@code onEnable} (the server then disables the plugin) or catch it to disable gracefully.
 *
 * <p>In embedded mode each plugin has its own relocated copy of Bloom, so {@link #get()} returns the instance of
 * the calling plugin.
 *
 * @since 0.1.0
 */
public final class Bloom {

    private static final Object LOCK = new Object();
    private static volatile Bloom current;

    private final Plugin plugin;
    private final BloomOptions options;
    private final Logger logger;
    private final CompositeTerminable resources;
    private volatile LifecycleState state = LifecycleState.NEW;

    private PlatformInfo platform;
    private PlatformAdapter adapter;
    private Capabilities capabilities;
    private DefaultServiceRegistry services;
    private TrackingScheduler scheduler;
    private Messenger messages;
    private Events events;
    private boolean untested;

    private Bloom(Plugin plugin, BloomOptions options) {
        this.plugin = plugin;
        this.options = options;
        this.logger = plugin.getLogger();
        this.resources = new CompositeTerminable(logger);
    }

    /**
     * Starts Bloom for a plugin with default options. Call from {@code onEnable()}.
     *
     * @param plugin the plugin that embeds Bloom
     * @return the running instance
     * @throws BloomStartupException if the platform is unsupported or the adapter is missing
     * @throws IllegalStateException if Bloom is already running for this plugin
     */
    public static Bloom enable(Plugin plugin) {
        return enable(plugin, BloomOptions.defaults());
    }

    /**
     * Starts Bloom for a plugin. Call from {@code onEnable()}.
     *
     * @param plugin the plugin that embeds Bloom
     * @param options startup options
     * @return the running instance
     * @throws BloomStartupException if the platform is unsupported or the adapter is missing
     * @throws IllegalStateException if Bloom is already running for this plugin
     */
    public static Bloom enable(Plugin plugin, BloomOptions options) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(options, "options");
        synchronized (LOCK) {
            Bloom existing = current;
            if (existing != null && existing.state == LifecycleState.RUNNING) {
                throw new IllegalStateException("Bloom is already running for plugin "
                        + existing.plugin.getName() + ". Call bloom.shutdown() in onDisable() before enabling again. "
                        + "If two different plugins hit this, one of them shaded Bloom without relocating it.");
            }
            Bloom bloom = new Bloom(plugin, options);
            bloom.start();
            current = bloom;
            return bloom;
        }
    }

    /**
     * Returns the running instance.
     *
     * @return the instance created by {@link #enable(Plugin)}
     * @throws IllegalStateException if Bloom is not running
     */
    public static Bloom get() {
        Bloom bloom = current;
        if (bloom == null || bloom.state != LifecycleState.RUNNING) {
            throw new IllegalStateException("Bloom is not running. Call Bloom.enable(this) in your plugin's onEnable() "
                    + "before using Bloom, and do not use it after bloom.shutdown().");
        }
        return bloom;
    }

    /**
     * Returns the running instance, if any.
     *
     * @return the instance, or empty if not running
     */
    public static Optional<Bloom> find() {
        Bloom bloom = current;
        return bloom != null && bloom.state == LifecycleState.RUNNING ? Optional.of(bloom) : Optional.<Bloom>empty();
    }

    private void start() {
        state = LifecycleState.STARTING;
        try {
            if (!plugin.getServer().isPrimaryThread()) {
                throw new BloomStartupException("Bloom.enable must be called on the main server thread (from "
                        + "onEnable), not from '" + Thread.currentThread().getName() + "'.");
            }
            platform = PlatformDetector.detect(plugin.getServer());
            SupportDecision decision = SupportPolicy.evaluate(
                    platform.software(),
                    platform.minecraftVersion(),
                    platform.javaRuntime(),
                    options.allowNewerUntestedVersions());
            if (!decision.isSupported()) {
                throw new BloomStartupException("Bloom " + BloomVersion.VERSION + " cannot run on "
                        + platform.describe() + ": " + decision.reason());
            }
            untested = decision.isUntested();
            adapter = AdapterLoader.load(decision.family(), platform);
            capabilities = adapter.enable(new AdapterContext(plugin, platform, logger, options.debug()));
            resources.bind(new AutoCloseable() {
                @Override
                public void close() {
                    adapter.disable();
                }
            });

            scheduler =
                    resources.bind(new TrackingScheduler(new BukkitSchedulerBackend(plugin), logger, plugin.getName()));
            events = resources.bind(new Events(plugin));
            Consumer<AutoCloseable> closeOnShutdown = new Consumer<AutoCloseable>() {
                @Override
                public void accept(AutoCloseable resource) {
                    resources.bind(resource);
                }
            };
            messages = new Messenger(
                    adapter.text(),
                    adapter.players(),
                    capabilities,
                    logger,
                    closeOnShutdown,
                    options.textFormat(),
                    options.unsupportedFeaturePolicy());
            // Bound last so feature services (menus, storage) close before events, scheduler and the adapter.
            services = resources.bind(new DefaultServiceRegistry(logger));

            checkRelocation();
            logBanner();
            state = LifecycleState.RUNNING;
        } catch (RuntimeException | LinkageError e) {
            state = LifecycleState.FAILED;
            resources.close();
            if (e instanceof BloomStartupException) {
                logger.severe(e.getMessage());
                throw (BloomStartupException) e;
            }
            throw new BloomStartupException(
                    "Bloom failed to start on " + (platform == null ? "an undetected platform" : platform.describe())
                            + ": " + e,
                    e);
        }
    }

    /**
     * Shuts Bloom down: closes feature services (open menus are closed, databases flushed), unregisters event
     * subscriptions, cancels scheduled tasks and disables the adapter. Call from {@code onDisable()}. Idempotent.
     */
    public void shutdown() {
        synchronized (LOCK) {
            if (state != LifecycleState.RUNNING) {
                return;
            }
            state = LifecycleState.STOPPING;
            resources.close();
            state = LifecycleState.STOPPED;
            if (current == this) {
                current = null;
            }
        }
        if (options.debug()) {
            logger.info("Bloom shut down.");
        }
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
     * Returns the options Bloom was started with.
     *
     * @return options
     */
    public BloomOptions options() {
        return options;
    }

    /**
     * Returns the lifecycle state.
     *
     * @return state
     */
    public LifecycleState state() {
        return state;
    }

    /**
     * Returns the detected platform.
     *
     * @return platform info
     */
    public PlatformInfo platform() {
        ensureRunning();
        return platform;
    }

    /**
     * Returns the capabilities of the running platform.
     *
     * @return capabilities
     */
    public Capabilities capabilities() {
        ensureRunning();
        return capabilities;
    }

    /**
     * Shortcut for {@code capabilities().has(capability)}.
     *
     * @param capability capability to check
     * @return {@code true} if available
     */
    public boolean has(Capability capability) {
        return capabilities().has(capability);
    }

    /**
     * Returns the active version adapter. <strong>Advanced:</strong> intended for Bloom modules and diagnostics.
     *
     * @return adapter
     */
    public PlatformAdapter adapter() {
        ensureRunning();
        return adapter;
    }

    /**
     * Returns the task scheduler. Tasks are cancelled on shutdown.
     *
     * @return scheduler
     */
    public BloomScheduler scheduler() {
        ensureRunning();
        return scheduler;
    }

    /**
     * Returns the default messenger, using the text format from {@link BloomOptions}.
     *
     * @return messenger
     */
    public Messenger messages() {
        ensureRunning();
        return messages;
    }

    /**
     * Returns the event subscription service. Subscriptions are removed on shutdown.
     *
     * @return events
     */
    public Events events() {
        ensureRunning();
        return events;
    }

    /**
     * Returns the service registry. Feature modules register their services here on first use (for example
     * {@code Menus.of(bloom)}); closeable services are closed on shutdown in reverse order.
     *
     * @return services
     */
    public ServiceRegistry services() {
        ensureRunning();
        return services;
    }

    /**
     * Shortcut for {@code services().require(type)}.
     *
     * @param type service key type
     * @param <T> service type
     * @return the service
     */
    public <T> T service(Class<T> type) {
        return services().require(type);
    }

    /**
     * Returns whether the server is newer than the supported range and Bloom only started because of
     * {@link BloomOptions.Builder#allowNewerUntestedVersions(boolean)}.
     *
     * @return {@code true} on an explicitly allowed untested version
     */
    public boolean isUntestedPlatform() {
        ensureRunning();
        return untested;
    }

    private void ensureRunning() {
        LifecycleState s = state;
        if (s != LifecycleState.RUNNING && s != LifecycleState.STARTING) {
            throw new IllegalStateException("Bloom is " + s + " for plugin " + plugin.getName()
                    + "; it can only be used between Bloom.enable() and bloom.shutdown().");
        }
    }

    private void checkRelocation() {
        ClassLoader bloomLoader = Bloom.class.getClassLoader();
        ClassLoader pluginLoader = plugin.getClass().getClassLoader();
        if (bloomLoader != pluginLoader) {
            logger.warning("Bloom classes were loaded by a different class loader than " + plugin.getName()
                    + " (" + bloomLoader + "). Another plugin probably shades Bloom without relocating it. Relocate "
                    + "'io.github.crownalonedev.bloom' in your shadow configuration to avoid version conflicts.");
        } else if (Bloom.class.getName().startsWith("io.github.crownalonedev.bloom.") && options.debug()) {
            logger.info("Bloom is not relocated. Relocate 'io.github.crownalonedev.bloom' when shading to avoid "
                    + "conflicts with other plugins that embed Bloom.");
        }
    }

    private void logBanner() {
        String header =
                "Bloom " + BloomVersion.VERSION + " · " + platform.describe() + " · adapter: " + adapter.description();
        if (options.quietStartup()) {
            logger.info(header);
        } else {
            logger.info(header);
            logger.info("  capabilities: " + names(capabilities.available()));
            logger.info("  unavailable:  " + names(capabilities.unavailable()));
        }
        if (untested) {
            logger.log(
                    Level.WARNING,
                    "Minecraft " + platform.minecraftVersion() + " is newer than this Bloom release "
                            + "supports (newest: " + SupportPolicy.MODERN.get(SupportPolicy.MODERN.size() - 1)
                            + "). Running "
                            + "anyway because allowNewerUntestedVersions is enabled. Test your plugin carefully.");
        }
    }

    private static String names(Iterable<Capability> capabilitySet) {
        List<String> names = new ArrayList<String>();
        for (Capability capability : capabilitySet) {
            names.add(capability.name());
        }
        return names.isEmpty() ? "(none)" : String.join(", ", names);
    }
}
