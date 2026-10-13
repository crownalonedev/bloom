package io.github.crownalonedev.bloom.api.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crownalonedev.bloom.api.UnsupportedCapabilityException;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SupportPolicyTest {

    private static final JavaRuntime JAVA8 = JavaRuntime.of("1.8", "1.8.0_472", "test");
    private static final JavaRuntime JAVA21 = JavaRuntime.of("21", "21.0.11", "test");
    private static final JavaRuntime JAVA25 = JavaRuntime.of("25", "25.0.1", "test");

    @Test
    void legacyVersionsSelectLegacyAdapter() {
        for (String v : new String[] {"1.7.10", "1.8", "1.8.8", "1.8.9"}) {
            SupportDecision d = SupportPolicy.evaluate(ServerSoftware.SPIGOT, MinecraftVersion.parse(v), JAVA8, false);
            assertTrue(d.isSupported(), v);
            assertEquals(PlatformFamily.LEGACY, d.family());
        }
    }

    @Test
    void oldOneSevenIsRejected() {
        SupportDecision d =
                SupportPolicy.evaluate(ServerSoftware.CRAFTBUKKIT, MinecraftVersion.of(1, 7, 2), JAVA8, false);
        assertFalse(d.isSupported());
        assertTrue(d.reason().contains("1.7.10"));
    }

    @Test
    void modernPaperSelectsModernAdapter() {
        assertEquals(
                PlatformFamily.MODERN,
                SupportPolicy.evaluate(ServerSoftware.PAPER, MinecraftVersion.V1_21_11, JAVA21, false)
                        .family());
        assertEquals(
                PlatformFamily.MODERN,
                SupportPolicy.evaluate(ServerSoftware.PAPER, MinecraftVersion.of(26, 3, 1), JAVA25, false)
                        .family());
    }

    @Test
    void modernSpigotIsRejectedWithExplanation() {
        SupportDecision d = SupportPolicy.evaluate(ServerSoftware.SPIGOT, MinecraftVersion.V1_21_11, JAVA21, false);
        assertFalse(d.isSupported());
        assertTrue(d.reason().contains("requires Paper"));
    }

    @Test
    void middleVersionsAreUnsupported() {
        SupportDecision d = SupportPolicy.evaluate(ServerSoftware.PAPER, MinecraftVersion.of(1, 12, 2), JAVA8, false);
        assertFalse(d.isSupported());
        assertThrows(IllegalStateException.class, d::family);
    }

    @Test
    void newerVersionsRequireExplicitOptIn() {
        MinecraftVersion future = MinecraftVersion.of(26, 4);
        assertFalse(SupportPolicy.evaluate(ServerSoftware.PAPER, future, JAVA25, false)
                .isSupported());
        SupportDecision optedIn = SupportPolicy.evaluate(ServerSoftware.PAPER, future, JAVA25, true);
        assertTrue(optedIn.isSupported());
        assertTrue(optedIn.isUntested());
    }

    @Test
    void foliaIsRejected() {
        assertFalse(SupportPolicy.evaluate(ServerSoftware.FOLIA, MinecraftVersion.V1_21_11, JAVA21, false)
                .isSupported());
    }

    @Test
    void capabilitiesRequireThrowsHelpfulError() {
        Capabilities caps = Capabilities.of(Collections.singleton(Capability.TITLES), "Spigot 1.8.8");
        assertTrue(caps.has(Capability.TITLES));
        assertTrue(caps.unavailable().contains(Capability.BOSS_BAR));
        UnsupportedCapabilityException e = assertThrows(
                UnsupportedCapabilityException.class, () -> caps.require(Capability.BOSS_BAR, "Messenger.bossBar"));
        assertEquals(Capability.BOSS_BAR, e.capability());
        assertTrue(e.getMessage().contains("Spigot 1.8.8"));
    }
}
