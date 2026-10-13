package io.github.crownalonedev.bloom.bukkit.event;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

/**
 * Configures an event subscription. Created by {@link Events#listen(Class)}.
 *
 * @param <E> event type
 * @since 0.1.0
 */
public final class SubscriptionBuilder<E extends Event> {

    private final Events events;
    private final Class<E> type;
    private EventPriority priority = EventPriority.NORMAL;
    private boolean ignoreCancelled;
    private Predicate<? super E> filter;
    private long limit;

    SubscriptionBuilder(Events events, Class<E> type) {
        this.events = events;
        this.type = type;
    }

    /**
     * Sets the listener priority.
     *
     * @param newPriority priority (default {@link EventPriority#NORMAL})
     * @return this builder
     */
    public SubscriptionBuilder<E> priority(EventPriority newPriority) {
        this.priority = Objects.requireNonNull(newPriority, "priority");
        return this;
    }

    /**
     * Skips events that another listener already cancelled.
     *
     * @return this builder
     */
    public SubscriptionBuilder<E> ignoreCancelled() {
        this.ignoreCancelled = true;
        return this;
    }

    /**
     * Only handles events that match a predicate. Multiple calls combine with logical AND.
     *
     * @param predicate condition
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    public SubscriptionBuilder<E> filter(Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (filter == null) {
            filter = predicate;
        } else {
            final Predicate<E> first = (Predicate<E>) filter;
            filter = first.and(predicate);
        }
        return this;
    }

    /**
     * Unsubscribes automatically after the handler processed a number of (filtered) events.
     *
     * @param maxCalls positive number of calls
     * @return this builder
     */
    public SubscriptionBuilder<E> limit(long maxCalls) {
        if (maxCalls < 1) {
            throw new IllegalArgumentException("limit must be at least 1, got " + maxCalls);
        }
        this.limit = maxCalls;
        return this;
    }

    /**
     * Registers the handler.
     *
     * @param handler event handler
     * @return the active subscription
     */
    public Subscription handler(Consumer<? super E> handler) {
        return events.register(
                type, priority, ignoreCancelled, filter, limit, Objects.requireNonNull(handler, "handler"));
    }
}
