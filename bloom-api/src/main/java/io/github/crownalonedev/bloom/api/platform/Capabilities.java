package io.github.crownalonedev.bloom.api.platform;

import io.github.crownalonedev.bloom.api.UnsupportedCapabilityException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * An immutable set of {@link Capability capabilities} available on the running platform.
 *
 * @since 0.1.0
 */
public final class Capabilities {

    private final Set<Capability> present;
    private final String platformDescription;

    private Capabilities(Set<Capability> present, String platformDescription) {
        this.present = present;
        this.platformDescription = platformDescription;
    }

    /**
     * Creates a capability set.
     *
     * @param present available capabilities
     * @param platformDescription description of the platform, used in error messages
     * @return the capability set
     */
    public static Capabilities of(Collection<Capability> present, String platformDescription) {
        EnumSet<Capability> copy = EnumSet.noneOf(Capability.class);
        copy.addAll(present);
        return new Capabilities(Collections.unmodifiableSet(copy), platformDescription);
    }

    /**
     * Creates an empty capability set.
     *
     * @param platformDescription description of the platform
     * @return a set with no capabilities
     */
    public static Capabilities none(String platformDescription) {
        return of(Collections.<Capability>emptySet(), platformDescription);
    }

    /**
     * Returns whether a capability is available.
     *
     * @param capability capability to check
     * @return {@code true} if available
     */
    public boolean has(Capability capability) {
        return present.contains(capability);
    }

    /**
     * Throws if a capability is unavailable.
     *
     * @param capability required capability
     * @param feature description of the feature being used, included in the error message
     * @throws UnsupportedCapabilityException if the capability is unavailable
     */
    public void require(Capability capability, String feature) {
        if (!has(capability)) {
            throw new UnsupportedCapabilityException(capability, platformDescription, feature);
        }
    }

    /**
     * Returns the available capabilities.
     *
     * @return unmodifiable set
     */
    public Set<Capability> available() {
        return present;
    }

    /**
     * Returns the capabilities that are not available.
     *
     * @return unmodifiable set
     */
    public Set<Capability> unavailable() {
        EnumSet<Capability> missing = EnumSet.allOf(Capability.class);
        missing.removeAll(present);
        return Collections.unmodifiableSet(missing);
    }

    /**
     * Returns the platform description used in error messages.
     *
     * @return platform description
     */
    public String platformDescription() {
        return platformDescription;
    }

    @Override
    public String toString() {
        return "Capabilities" + present;
    }
}
