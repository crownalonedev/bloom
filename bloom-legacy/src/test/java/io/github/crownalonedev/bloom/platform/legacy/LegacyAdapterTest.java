package io.github.crownalonedev.bloom.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crownalonedev.bloom.api.text.TextFormat;
import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the parts of the legacy adapter that do not need a running server. Behaviour against real 1.7.10 /
 * 1.8.8 servers is verified by the real-server harness (scripts/realserver).
 */
class LegacyAdapterTest {

    @Test
    void extractsNmsVersionPackage() {
        assertEquals("v1_8_R3", LegacyNms.versionPackage("org.bukkit.craftbukkit.v1_8_R3.CraftServer"));
        assertEquals("v1_7_R4", LegacyNms.versionPackage("org.bukkit.craftbukkit.v1_7_R4.CraftServer"));
        assertNull(LegacyNms.versionPackage("org.bukkit.craftbukkit.CraftServer"));
        assertNull(LegacyNms.versionPackage("org.mockbukkit.mockbukkit.ServerMock"));
    }

    @Test
    void jsonEscaping() {
        assertEquals("\"§aHi \\\"you\\\"\\n\\\\\"", LegacyNms.jsonString("§aHi \"you\"\n\\"));
        assertEquals("\"\\u0001\"", LegacyNms.jsonString("\u0001"));
    }

    @Test
    void onlinePlayersAdaptsArrayAndCollection() {
        assertEquals(0, LegacyPlayers.adapt(new Player[0]).size());
        Collection<Player> list = new ArrayList<>();
        assertEquals(0, LegacyPlayers.adapt(list).size());
        assertThrows(IllegalStateException.class, () -> LegacyPlayers.adapt("nope"));
    }

    @Test
    void titlesAreTruncatedTo32Characters() {
        LegacyPlayers players = new LegacyPlayers(Logger.getAnonymousLogger());
        String fitted = players.fitTitle("§d§lA very long inventory title that overflows");
        assertEquals(32, fitted.length());
        assertEquals("§dShort", players.fitTitle("§dShort"));
        assertTrue(players.fitTitle("§d0123456789012345678901234567890§c").length() <= 32);
    }

    @Test
    void renderConvertsMiniMessageAndResetsItemText() {
        assertEquals(
                "§dHi",
                LegacyTextPlatform.render(FormattedText.ofPrepared("<light_purple>Hi", TextFormat.MINIMESSAGE)));
        assertEquals("§rPlain", LegacyItems.itemText(FormattedText.ofPrepared("Plain", TextFormat.LEGACY)));
        assertEquals("§aGreen", LegacyItems.itemText(FormattedText.ofPrepared("§aGreen", TextFormat.LEGACY)));
    }
}
