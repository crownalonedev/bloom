package io.github.crownalonedev.bloom.bukkit.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crownalonedev.bloom.api.BloomStartupException;
import io.github.crownalonedev.bloom.api.platform.JavaRuntime;
import io.github.crownalonedev.bloom.api.platform.MinecraftVersion;
import io.github.crownalonedev.bloom.api.platform.PlatformFamily;
import io.github.crownalonedev.bloom.api.platform.PlatformInfo;
import io.github.crownalonedev.bloom.api.platform.ServerSoftware;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlatformDetectorTest {

    private static PlatformDetector.ClassProbe classes(String... names) {
        Set<String> present = new HashSet<>(Arrays.asList(names));
        return present::contains;
    }

    @Test
    void detectsLegacySpigot() {
        PlatformInfo info = PlatformDetector.detect(
                "CraftBukkit",
                "git-Spigot-db6de12-18fbb24 (MC: 1.8.8)",
                "1.8.8-R0.1-SNAPSHOT",
                "org.bukkit.craftbukkit.v1_8_R3.CraftServer",
                classes("org.spigotmc.SpigotConfig"),
                JavaRuntime.of("1.8", "1.8.0_472", "t"));
        assertEquals(ServerSoftware.SPIGOT, info.software());
        assertEquals(MinecraftVersion.V1_8_8, info.minecraftVersion());
        assertEquals("Spigot 1.8.8 (Java 1.8.0_472)", info.describe());
    }

    @Test
    void detectsPaperSpigotAndCraftBukkit() {
        assertEquals(
                ServerSoftware.PAPER_SPIGOT,
                PlatformDetector.detectSoftware(
                        "CraftBukkit",
                        "x",
                        classes("org.github.paperspigot.PaperSpigotConfig", "org.spigotmc.SpigotConfig")));
        assertEquals(ServerSoftware.CRAFTBUKKIT, PlatformDetector.detectSoftware("CraftBukkit", "x", classes()));
        assertEquals(ServerSoftware.UNKNOWN, PlatformDetector.detectSoftware("Glowstone", "x", classes()));
    }

    @Test
    void detectsModernPaperForksFoliaAndMockBukkit() {
        assertEquals(
                ServerSoftware.PAPER,
                PlatformDetector.detectSoftware(
                        "Paper", "x", classes("io.papermc.paper.ServerBuildInfo", "org.spigotmc.SpigotConfig")));
        assertEquals(
                ServerSoftware.PAPER_FORK,
                PlatformDetector.detectSoftware("Purpur", "x", classes("io.papermc.paper.ServerBuildInfo")));
        assertEquals(
                ServerSoftware.FOLIA,
                PlatformDetector.detectSoftware(
                        "Folia",
                        "x",
                        classes(
                                "io.papermc.paper.threadedregions.RegionizedServer",
                                "io.papermc.paper.ServerBuildInfo")));
        assertEquals(
                ServerSoftware.MOCKBUKKIT,
                PlatformDetector.detectSoftware(
                        "ServerMock",
                        "org.mockbukkit.mockbukkit.ServerMock",
                        classes("io.papermc.paper.ServerBuildInfo")));
    }

    @Test
    void undetectableVersionFailsWithDetails() {
        BloomStartupException e = assertThrows(
                BloomStartupException.class,
                () -> PlatformDetector.detect("Weird", "custom", "unknown", "x", classes(), JavaRuntime.current()));
        assertTrue(e.getMessage().contains("name='Weird'"));
    }

    @Test
    void adapterClassNamesFollowRuntimePackage() {
        assertEquals(
                "io.github.crownalonedev.bloom.platform.legacy.LegacyPlatformAdapter",
                AdapterLoader.adapterClassName(PlatformFamily.LEGACY));
        assertEquals(
                "com.example.libs.bloom.platform.modern.ModernPlatformAdapter",
                AdapterLoader.adapterClassName(
                        "com.example.libs.bloom.bukkit.platform.AdapterLoader", PlatformFamily.MODERN));
    }
}
