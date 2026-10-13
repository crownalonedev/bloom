package io.github.crownalonedev.bloom.common.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.api.text.TextFormat;
import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void colorizeTranslatesCodesAndHex() {
        assertEquals("§dHi §lthere", LegacyText.colorize("&dHi &Lthere", false));
        assertEquals("& alone &z", LegacyText.colorize("& alone &z", false));
        assertEquals("§x§f§2§a§7§c§cpink", LegacyText.colorize("&#F2A7CCpink", true));
        assertEquals("§dpink", LegacyText.colorize("&#F2A7CCpink", false));
    }

    @Test
    void stripAndTruncate() {
        assertEquals("Hello", LegacyText.strip("§d§lHel§x§f§f§f§f§f§flo"));
        assertEquals("§dab", LegacyText.truncate("§dab§c", 5));
    }

    @Test
    void miniMessageColorsAndDecorations() {
        assertEquals("§cHello", MiniMessageLite.toLegacy("<red>Hello"));
        assertEquals("§cA§lB§cC", MiniMessageLite.toLegacy("<red>A<bold>B</bold>C"));
        assertEquals("§aok§rplain", MiniMessageLite.toLegacy("<green>ok</green>plain"));
        assertEquals("§dx", MiniMessageLite.toLegacy("<color:light_purple>x"));
        assertEquals("§7grey", MiniMessageLite.toLegacy("<grey>grey"));
        assertEquals("§lb§r§oi", MiniMessageLite.toLegacy("<b>b<!b><i>i"));
    }

    @Test
    void miniMessageHexDownsamples() {
        assertEquals("§dSakura", MiniMessageLite.toLegacy("<#F2A7CC>Sakura"));
        assertEquals("§dSakura", MiniMessageLite.toLegacy("<gradient:#F2A7CC:#B9A0DD>Sakura</gradient>"));
    }

    @Test
    void miniMessageEffectsKeepContent() {
        assertEquals("§eclick me", MiniMessageLite.toLegacy("<yellow><click:run_command:'/spawn'>click me</click>"));
        assertEquals("§atip", MiniMessageLite.toLegacy("<hover:show_text:'<red>hi'><green>tip"));
    }

    @Test
    void miniMessageUnknownTagsAndEscapes() {
        assertEquals("<unknown>text", MiniMessageLite.toLegacy("<unknown>text"));
        assertEquals("<red> literal", MiniMessageLite.toLegacy("\\<red> literal"));
        assertEquals("a < b", MiniMessageLite.toLegacy("a < b"));
        assertEquals("line1\nline2", MiniMessageLite.toLegacy("line1<newline>line2"));
        assertEquals("§c<bold>raw", MiniMessageLite.toLegacy("<red><pre><bold>raw</pre>"));
    }

    @Test
    void miniMessageReset() {
        assertEquals("§c§lA§rB", MiniMessageLite.toLegacy("<red><bold>A<reset>B"));
    }

    @Test
    void placeholdersAreEscapedForEachFormat() {
        assertEquals(
                "<gray>Hi \\<red>Bob",
                TextTemplates.prepare(
                        "<gray>Hi {name}", TextFormat.MINIMESSAGE, false, Placeholder.of("name", "<red>Bob")));
        assertEquals(
                "§7Hi &cBob",
                TextTemplates.prepare("&7Hi {name}", TextFormat.LEGACY, false, Placeholder.of("name", "&cBob")));
        assertEquals(
                "§7Hi Bob",
                TextTemplates.prepare("&7Hi {name}", TextFormat.LEGACY, false, Placeholder.of("name", "§cBob")));
        assertEquals(
                "§7Hi §cBob",
                TextTemplates.prepare("&7Hi {name}", TextFormat.LEGACY, false, Placeholder.formatted("name", "&cBob")));
    }

    @Test
    void placeholdersAreSinglePassAndUnknownKeysRemain() {
        assertEquals(
                "{b} and {missing}",
                TextTemplates.prepare(
                        "{a} and {missing}",
                        TextFormat.MINIMESSAGE,
                        false,
                        Placeholder.of("a", "{b}"),
                        Placeholder.of("b", "x")));
    }
}
