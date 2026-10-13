package io.github.crownalonedev.bloom.storage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A minimal asynchronous repository. Bloom provides {@link SqlRepository} as a simple SQL implementation; implement
 * this interface yourself for other stores (files, Redis, ...) so callers do not depend on the storage technology.
 *
 * @param <K> key type
 * @param <V> value type
 * @since 0.1.0
 */
public interface Repository<K, V> {

    /**
     * Loads a value.
     *
     * @param key key
     * @return future with the value, or empty
     */
    CompletableFuture<Optional<V>> find(K key);

    /**
     * Loads all values. Avoid on large tables.
     *
     * @return future with all values
     */
    CompletableFuture<List<V>> findAll();

    /**
     * Inserts or updates a value.
     *
     * @param value value
     * @return future completed when saved
     */
    CompletableFuture<Void> save(V value);

    /**
     * Deletes a value.
     *
     * @param key key
     * @return future with {@code true} if a value was deleted
     */
    CompletableFuture<Boolean> delete(K key);
}
