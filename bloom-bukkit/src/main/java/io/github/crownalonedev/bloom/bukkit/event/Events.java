package io.github.crownalonedev.bloom.bukkit.event;

import io.github.crownalonedev.bloom.api.BloomException;
import io.github.crownalonedev.bloom.api.lifecycle.Terminable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

/**
 * Lambda-based event subscriptions that are automatically removed when Bloom shuts down.
 *
 * <pre>{@code
 * Events events = bloom.events();
 *
 * events.subscribe(PlayerJoinEvent.class, event -> messages.send(event.getPlayer(), "&dWelcome!"));
 *
 * events.listen(PlayerInteractEvent.class)
 *         .priority(EventPriority.HIGH)
 *         .ignoreCancelled()
 *         .filter(event -> event.getItem() != null)
 *         .limit(1)
 *         .handler(event -> event.getPlayer().sendMessage("First interaction!"));
 * }</pre>
 *
 * <p>Handlers run on the thread that fires the event: the main thread for most events, a background thread for
 * asynchronous events such as {@code AsyncPlayerPreLoginEvent}. Exceptions thrown by handlers are reported by
 * the server with the plugin's name, like any listener exception.
 *
 * @since 0.1.0
 */
public final class Events implements Terminable {

    private final Plugin plugin;
    private final Set<Registration> active = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    /**
     * Creates the service. <strong>Internal:</strong> obtain it from {@code bloom.events()}.
     *
     * @param plugin owning plugin
     */
    public Events(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Subscribes a handler at {@link EventPriority#NORMAL}, receiving cancelled events too.
     *
     * @param type concrete event class (must declare a {@code HandlerList})
     * @param handler handler
     * @param <E> event type
     * @return the subscription
     */
    public <E extends Event> Subscription subscribe(Class<E> type, Consumer<? super E> handler) {
        return listen(type).handler(handler);
    }

    /**
     * Starts building a subscription with priority, filters and limits.
     *
     * @param type concrete event class
     * @param <E> event type
     * @return a builder; call {@link SubscriptionBuilder#handler(Consumer)} to register
     */
    public <E extends Event> SubscriptionBuilder<E> listen(Class<E> type) {
        return new SubscriptionBuilder<E>(this, Objects.requireNonNull(type, "type"));
    }

    /**
     * Registers a classic {@link Listener} with {@code @EventHandler} methods, tied to Bloom's lifecycle.
     *
     * @param listener listener instance
     * @return a subscription that unregisters the listener
     */
    public Subscription register(final Listener listener) {
        ensureOpen();
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        Registration registration = new Registration(listener);
        active.add(registration);
        return registration;
    }

    /**
     * Returns the number of active subscriptions created through this service.
     *
     * @return active count
     */
    public int activeCount() {
        return active.size();
    }

    /** Unregisters every subscription. Idempotent. */
    @Override
    public void close() {
        closed = true;
        List<Registration> snapshot = new ArrayList<Registration>(active);
        for (Registration registration : snapshot) {
            registration.unsubscribe();
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    <E extends Event> Subscription register(
            final Class<E> type,
            EventPriority priority,
            boolean ignoreCancelled,
            final Predicate<? super E> filter,
            final long limit,
            final Consumer<? super E> handler) {
        ensureOpen();
        final Registration registration = new Registration(new Listener() {});
        EventExecutor executor = new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) {
                if (!registration.isActive() || !type.isInstance(event)) {
                    return;
                }
                E typed = type.cast(event);
                if (filter != null && !filter.test(typed)) {
                    return;
                }
                long count = registration.handled.incrementAndGet();
                if (limit > 0 && count >= limit) {
                    registration.unsubscribe();
                }
                if (limit <= 0 || count <= limit) {
                    handler.accept(typed);
                }
            }
        };
        try {
            plugin.getServer()
                    .getPluginManager()
                    .registerEvent(type, registration.listener, priority, executor, plugin, ignoreCancelled);
        } catch (RuntimeException e) {
            throw new BloomException(
                    "Cannot subscribe to " + type.getName() + ". Abstract events without a "
                            + "HandlerList (e.g. PlayerEvent) cannot be listened to; subscribe to a concrete event such as "
                            + "PlayerJoinEvent.",
                    e);
        }
        active.add(registration);
        return registration;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Cannot subscribe: Bloom has shut down.");
        }
    }

    private final class Registration implements Subscription {
        final Listener listener;
        final AtomicLong handled = new AtomicLong();
        private final AtomicBoolean activeFlag = new AtomicBoolean(true);

        Registration(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void unsubscribe() {
            if (activeFlag.compareAndSet(true, false)) {
                HandlerList.unregisterAll(listener);
                active.remove(this);
            }
        }

        @Override
        public boolean isActive() {
            return activeFlag.get();
        }

        @Override
        public long handledCount() {
            return handled.get();
        }
    }
}
