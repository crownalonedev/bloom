package io.github.crownalonedev.bloom.common.service;

import io.github.crownalonedev.bloom.api.BloomException;
import io.github.crownalonedev.bloom.api.lifecycle.Terminable;
import io.github.crownalonedev.bloom.api.service.ServiceRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Default {@link ServiceRegistry}: a synchronized, insertion-ordered map that closes services on shutdown. */
public final class DefaultServiceRegistry implements ServiceRegistry, Terminable {

    private final Logger logger;
    private final Map<Class<?>, Object> services = new LinkedHashMap<Class<?>, Object>();
    private boolean closed;

    /**
     * Creates an empty registry.
     *
     * @param logger receives errors raised while closing services
     */
    public DefaultServiceRegistry(Logger logger) {
        this.logger = logger;
    }

    @Override
    public synchronized <T> void register(Class<T> type, T service) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(service, "service");
        ensureOpen();
        if (!type.isInstance(service)) {
            throw new IllegalArgumentException(service.getClass().getName() + " is not a " + type.getName());
        }
        if (services.containsKey(type)) {
            throw new IllegalStateException("A service is already registered for " + type.getName()
                    + ". Use find()/require() to obtain it, or getOrRegister() for lazy creation.");
        }
        services.put(type, service);
    }

    @Override
    public synchronized <T> Optional<T> find(Class<T> type) {
        return Optional.ofNullable(type.cast(services.get(type)));
    }

    @Override
    public synchronized <T> T require(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            List<String> names = new ArrayList<String>();
            for (Class<?> registered : services.keySet()) {
                names.add(registered.getSimpleName());
            }
            throw new BloomException("No service registered for " + type.getName() + ". Registered services: " + names
                    + ". Feature modules register their service on first use, e.g. Menus.of(bloom).");
        }
        return type.cast(service);
    }

    @Override
    public synchronized <T> T getOrRegister(Class<T> type, Supplier<? extends T> factory) {
        Object existing = services.get(type);
        if (existing != null) {
            return type.cast(existing);
        }
        ensureOpen();
        T created = Objects.requireNonNull(factory.get(), "Service factory returned null for " + type.getName());
        register(type, created);
        return created;
    }

    @Override
    public synchronized boolean isRegistered(Class<?> type) {
        return services.containsKey(type);
    }

    @Override
    public synchronized Set<Class<?>> types() {
        return Collections.unmodifiableSet(new LinkedHashSet<Class<?>>(services.keySet()));
    }

    /** Closes closeable services in reverse registration order and clears the registry. Idempotent. */
    @Override
    public void close() {
        List<Map.Entry<Class<?>, Object>> entries;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            entries = new ArrayList<Map.Entry<Class<?>, Object>>(services.entrySet());
            services.clear();
        }
        Collections.reverse(entries);
        Set<Object> closedInstances = Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>());
        for (Map.Entry<Class<?>, Object> entry : entries) {
            Object service = entry.getValue();
            if (!(service instanceof AutoCloseable) || !closedInstances.add(service)) {
                continue;
            }
            try {
                ((AutoCloseable) service).close();
            } catch (Throwable t) {
                logger.log(
                        Level.SEVERE,
                        "Failed to close service " + entry.getKey().getName(),
                        t);
            }
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Service registry is closed: Bloom has shut down.");
        }
    }
}
