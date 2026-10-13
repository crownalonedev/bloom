package io.github.crownalonedev.bloom.api;

import io.github.crownalonedev.bloom.api.platform.Capability;

/**
 * Thrown when code requires a platform capability that the running server does not provide, for example
 * boss bars on a 1.8 server.
 *
 * <p>Avoid this exception by checking {@code capabilities().has(capability)} first, or by choosing an API
 * variant with a documented fallback.
 *
 * @since 0.1.0
 */
public class UnsupportedCapabilityException extends BloomException {

    private static final long serialVersionUID = 1L;

    private final Capability capability;

    /**
     * Creates the exception.
     *
     * @param capability the missing capability
     * @param platformDescription human-readable description of the running platform
     * @param feature what the caller attempted, e.g. {@code "Messenger.actionBar"}
     */
    public UnsupportedCapabilityException(Capability capability, String platformDescription, String feature) {
        super(feature + " requires capability " + capability.name() + " (" + capability.description()
                + "), which is not available on " + platformDescription + ". Check capabilities().has(Capability."
                + capability.name() + ") before calling, or use an API with a documented fallback. "
                + "See docs: compatibility.");
        this.capability = capability;
    }

    /**
     * Returns the capability that was required.
     *
     * @return the missing capability
     */
    public Capability capability() {
        return capability;
    }
}
