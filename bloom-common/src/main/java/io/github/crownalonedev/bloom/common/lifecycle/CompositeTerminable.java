package io.github.crownalonedev.bloom.common.lifecycle;

import io.github.crownalonedev.bloom.api.lifecycle.Terminable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collects resources and closes them in reverse order of registration. Failures while closing one resource are
 * logged and do not prevent the others from closing.
 */
public final class CompositeTerminable implements Terminable {

    private final Logger logger;
    private final Deque<AutoCloseable> resources = new ArrayDeque<AutoCloseable>();
    private boolean closed;

    /**
     * Creates an empty composite.
     *
     * @param logger receives errors raised while closing resources
     */
    public CompositeTerminable(Logger logger) {
        this.logger = logger;
    }

    /**
     * Adds a resource. If the composite is already closed, the resource is closed immediately.
     *
     * @param resource resource to close later
     * @param <T> resource type
     * @return the same resource, for chaining
     */
    public <T extends AutoCloseable> T bind(T resource) {
        boolean closeNow;
        synchronized (this) {
            closeNow = closed;
            if (!closeNow) {
                resources.push(resource);
            }
        }
        if (closeNow) {
            closeQuietly(resource);
        }
        return resource;
    }

    /**
     * Removes a resource without closing it.
     *
     * @param resource resource to forget
     */
    public synchronized void unbind(AutoCloseable resource) {
        resources.remove(resource);
    }

    @Override
    public void close() {
        List<AutoCloseable> toClose;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            toClose = new ArrayList<AutoCloseable>(resources);
            resources.clear();
        }
        for (AutoCloseable resource : toClose) {
            closeQuietly(resource);
        }
    }

    private void closeQuietly(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Failed to close " + resource, t);
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }
}
