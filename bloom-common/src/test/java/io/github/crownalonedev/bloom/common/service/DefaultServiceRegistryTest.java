package io.github.crownalonedev.bloom.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crownalonedev.bloom.api.BloomException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class DefaultServiceRegistryTest {

    interface Greeter {
        String greet();
    }

    static final class Closing implements AutoCloseable {
        final String name;
        final List<String> closed;

        Closing(String name, List<String> closed) {
            this.name = name;
            this.closed = closed;
        }

        @Override
        public void close() {
            closed.add(name);
        }
    }

    private final DefaultServiceRegistry registry = new DefaultServiceRegistry(Logger.getAnonymousLogger());

    @Test
    void registerFindRequire() {
        Greeter greeter = () -> "hi";
        registry.register(Greeter.class, greeter);
        assertSame(greeter, registry.require(Greeter.class));
        assertTrue(registry.find(Greeter.class).isPresent());
        assertThrows(IllegalStateException.class, () -> registry.register(Greeter.class, () -> "again"));
    }

    @Test
    void requireMissingListsRegisteredServices() {
        registry.register(String.class, "x");
        BloomException e = assertThrows(BloomException.class, () -> registry.require(Greeter.class));
        assertTrue(e.getMessage().contains("String"));
    }

    @Test
    void getOrRegisterCreatesOnce() {
        int[] calls = {0};
        Greeter a = registry.getOrRegister(Greeter.class, () -> {
            calls[0]++;
            return () -> "a";
        });
        Greeter b = registry.getOrRegister(Greeter.class, () -> {
            calls[0]++;
            return () -> "b";
        });
        assertSame(a, b);
        assertEquals(1, calls[0]);
    }

    @Test
    void closeRunsInReverseOrderAndRejectsLaterRegistration() {
        List<String> closed = new ArrayList<String>();
        registry.register(Closing.class, new Closing("first", closed));
        registry.register(Object.class, new Closing("second", closed));
        registry.close();
        assertEquals(Arrays.asList("second", "first"), closed);
        assertTrue(registry.isClosed());
        assertFalse(registry.isRegistered(Closing.class));
        assertThrows(IllegalStateException.class, () -> registry.register(String.class, "late"));
    }
}
