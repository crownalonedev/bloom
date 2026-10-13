package io.github.crownalonedev.bloom.api.service;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A simple, type-keyed registry of services owned by one Bloom instance.
 *
 * <p>Services that implement {@link io.github.crownalonedev.bloom.api.lifecycle.Terminable} or
 * {@link AutoCloseable} are closed automatically, in reverse registration order, when Bloom shuts down.
 *
 * <p>All methods are thread-safe.
 *
 * @since 0.1.0
 */
public interface ServiceRegistry {

    /**
     * Registers a service.
     *
     * @param type the key type, usually an interface the service implements
     * @param service the service instance
     * @param <T> service type
     * @throws IllegalStateException if a service is already registered for {@code type} or the registry is closed
     */
    <T> void register(Class<T> type, T service);

    /**
     * Looks up a service.
     *
     * @param type key type
     * @param <T> service type
     * @return the service, or empty if none is registered
     */
    <T> Optional<T> find(Class<T> type);

    /**
     * Looks up a service that must exist.
     *
     * @param type key type
     * @param <T> service type
     * @return the service
     * @throws io.github.crownalonedev.bloom.api.BloomException if no service is registered, with a message listing
     *     the registered services
     */
    <T> T require(Class<T> type);

    /**
     * Returns the registered service, atomically creating and registering it first if absent.
     *
     * @param type key type
     * @param factory creates the service; called at most once per type
     * @param <T> service type
     * @return the existing or newly created service
     */
    <T> T getOrRegister(Class<T> type, Supplier<? extends T> factory);

    /**
     * Returns whether a service is registered.
     *
     * @param type key type
     * @return {@code true} if registered
     */
    boolean isRegistered(Class<?> type);

    /**
     * Returns the registered key types in registration order.
     *
     * @return snapshot of registered types
     */
    Set<Class<?>> types();
}
