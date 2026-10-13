package io.github.crownalonedev.bloom.bukkit;

/**
 * What Bloom does when code uses a feature the running platform lacks, such as action bars on 1.7.10.
 *
 * @since 0.1.0
 */
public enum UnsupportedFeaturePolicy {
    /** Skip the operation and log one warning per feature. The method returns {@code false}. Default. */
    SKIP,
    /** Deliver text features (action bar, title) as a chat message instead; other features are skipped. */
    FALLBACK_TO_CHAT,
    /** Throw {@link io.github.crownalonedev.bloom.api.UnsupportedCapabilityException}. */
    THROW
}
