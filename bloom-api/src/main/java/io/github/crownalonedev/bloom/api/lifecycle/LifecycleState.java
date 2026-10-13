package io.github.crownalonedev.bloom.api.lifecycle;

/**
 * Lifecycle states of a Bloom instance.
 *
 * @since 0.1.0
 */
public enum LifecycleState {
    /** Created but not yet started. */
    NEW,
    /** Starting: detecting the platform and enabling the adapter. */
    STARTING,
    /** Running; all services are usable. */
    RUNNING,
    /** Shutting down: services are being closed. */
    STOPPING,
    /** Shut down. Services must not be used. */
    STOPPED,
    /** Startup failed; the instance cannot be used. */
    FAILED
}
