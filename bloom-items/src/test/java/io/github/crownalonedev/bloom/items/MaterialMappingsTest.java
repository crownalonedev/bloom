package io.github.crownalonedev.bloom.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaterialMappingsTest {

    private final MaterialMappings mappings = new MaterialMappings();

    @Test
    void modernNamesMapToLegacyDataValues() {
        assertEquals(
                "[WOOL:14, RED_WOOL]", mappings.candidates("RED_WOOL", true).toString());
        assertEquals(
                "[SKULL_ITEM:3, PLAYER_HEAD]",
                mappings.candidates("player_head", true).toString());
        assertEquals(
                "[LOG_2:1, DARK_OAK_LOG]",
                mappings.candidates("DARK_OAK_LOG", true).toString());
        assertEquals(
                "[INK_SACK:4, LAPIS_LAZULI]",
                mappings.candidates("minecraft:lapis_lazuli", true).toString());
        assertEquals("[WATCH, CLOCK]", mappings.candidates("CLOCK", true).toString());
    }

    @Test
    void legacySpecsWorkOnLegacyServers() {
        assertEquals("[WOOL:14]", mappings.candidates("WOOL:14", true).toString());
        assertEquals(
                "[DIAMOND_SWORD]", mappings.candidates("DIAMOND_SWORD", true).toString());
    }

    @Test
    void legacyNamesMapToModernNames() {
        assertEquals("[RED_WOOL]", mappings.candidates("WOOL:14", false).toString());
        assertEquals("[WATCH, CLOCK]", mappings.candidates("WATCH", false).toString());
        assertEquals("[WHITE_WOOL]", mappings.candidates("WOOL:0", false).toString());
        assertEquals("[WOOL, WHITE_WOOL]", mappings.candidates("WOOL", false).toString());
    }

    @Test
    void ambiguousNamesUseModernMeaning() {
        // MELON is the block on modern servers, so legacy servers get MELON_BLOCK first.
        assertEquals("[MELON_BLOCK, MELON]", mappings.candidates("MELON", true).toString());
        assertEquals("MELON", mappings.candidates("MELON", false).get(0).name());
        assertEquals("[EMPTY_MAP, MAP]", mappings.candidates("MAP", true).toString());
    }

    @Test
    void customMappingsWinOnBothFamilies() {
        mappings.register("shop_icon", "AMETHYST_SHARD", "DIAMOND", 0);
        assertEquals(
                "AMETHYST_SHARD", mappings.candidates("SHOP_ICON", false).get(0).name());
        assertEquals("DIAMOND", mappings.candidates("SHOP_ICON", true).get(0).name());
    }

    @Test
    void tableIsReasonablyComplete() {
        assertTrue(MaterialMappings.builtinSize() > 200, "size=" + MaterialMappings.builtinSize());
        assertThrows(IllegalArgumentException.class, () -> mappings.candidates("  ", true));
    }

    @Test
    void namespaceSanitising() {
        assertEquals("my_plugin", Items.namespaceOf("My Plugin"));
        assertEquals("hcf-core", Items.namespaceOf("HCF-Core"));
        assertThrows(IllegalArgumentException.class, () -> Items.checkKey("Bad Key"));
    }
}
