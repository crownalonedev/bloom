package io.github.crownalonedev.bloom.api.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinecraftVersionTest {

    @Test
    void parsesBukkitVersionStrings() {
        assertEquals(MinecraftVersion.of(1, 8, 8), MinecraftVersion.parse("1.8.8-R0.1-SNAPSHOT"));
        assertEquals(MinecraftVersion.of(1, 7, 10), MinecraftVersion.parse("1.7.10-R0.1-SNAPSHOT"));
        assertEquals(MinecraftVersion.of(1, 21, 11), MinecraftVersion.parse("1.21.11-R0.1-SNAPSHOT"));
        assertEquals(MinecraftVersion.of(1, 8, 0), MinecraftVersion.parse("1.8-R0.1-SNAPSHOT"));
    }

    @Test
    void parsesYearBasedVersions() {
        assertEquals(MinecraftVersion.of(26, 3, 0), MinecraftVersion.parse("26.3.build.177-stable"));
        assertEquals(MinecraftVersion.of(26, 1, 2), MinecraftVersion.parse("26.1.2"));
        assertEquals("26.3", MinecraftVersion.of(26, 3).toString());
    }

    @Test
    void ignoresPreReleaseSuffix() {
        assertEquals(MinecraftVersion.of(1, 21, 11), MinecraftVersion.parse("1.21.11-pre3"));
    }

    @Test
    void detectPrefersMcMarker() {
        assertEquals(
                MinecraftVersion.V1_8_8,
                MinecraftVersion.detect("1.8-R0.1-SNAPSHOT", "git-Spigot-db6de12-18fbb24 (MC: 1.8.8)")
                        .get());
        assertEquals(
                MinecraftVersion.of(1, 21, 11),
                MinecraftVersion.detect("garbage", "1.21.11-120-abc (MC: 1.21.11)")
                        .get());
        assertEquals(
                MinecraftVersion.of(26, 3),
                MinecraftVersion.detect("26.3.build.177-stable", null).get());
        assertFalse(MinecraftVersion.detect(null, "no version here").isPresent());
    }

    @Test
    void orderingWorksAcrossVersionSchemes() {
        assertTrue(MinecraftVersion.of(26, 1).isAtLeast(MinecraftVersion.V1_21_11));
        assertTrue(MinecraftVersion.V1_7_10.isBefore(MinecraftVersion.V1_8));
        assertTrue(MinecraftVersion.of(1, 8, 3).isSameLine(MinecraftVersion.V1_8_8));
        assertTrue(MinecraftVersion.V1_8_8.isBetween(MinecraftVersion.V1_7_10, MinecraftVersion.V1_8_9));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.parse("Paper"));
        assertFalse(MinecraftVersion.tryParse(null).isPresent());
    }

    @Test
    void javaRuntimeParsesBothSpecificationStyles() {
        assertEquals(8, JavaRuntime.of("1.8", "1.8.0_472", "Temurin").featureVersion());
        assertEquals(21, JavaRuntime.of("21", "21.0.11", "Temurin").featureVersion());
        assertEquals(25, JavaRuntime.of("25", "25", "Oracle").featureVersion());
    }
}
